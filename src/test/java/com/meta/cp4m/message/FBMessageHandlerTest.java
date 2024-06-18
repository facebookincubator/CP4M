/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.Service;
import com.meta.cp4m.ServicesRunner;
import com.meta.cp4m.message.Message.Role;
import com.meta.cp4m.plugin.DummyPlugin;
import com.meta.cp4m.store.MemoryStore;
import com.meta.cp4m.store.MemoryStoreConfig;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class FBMessageHandlerTest {
  /** Example message collected directly from the messenger webhook */
  public static final String SAMPLE_MESSAGE =
      "{\"object\":\"page\",\"entry\":[{\"id\":\"106195825075770\",\"time\":1692813219204,\"messaging\":[{\"sender\":{\"id\":\"6357858494326947\"},\"recipient\":{\"id\":\"106195825075770\"},\"timestamp\":1692813218705,\"message\":{\"mid\":\"m_kT_mWOSYh_eK3kF8chtyCWfcD9-gomvu4mhaMFQl-gt4D3LjORi6k3BXD6_x9a-FOUt-D2LFuywJN6HfrpAnDg\",\"text\":\"asdfa\"}}]}]}";

  public static final String SAMPLE_IG_MESSAGE =
      "{\"object\":\"instagram\",\"entry\":[{\"time\":1692813219204,\"id\":\"106195825075770\",\"messaging\":[{\"sender\":{\"id\":\"6357858494326947\"},\"recipient\":{\"id\":\"106195825075770\"},\"timestamp\":1692813218705,\"message\":{\"mid\":\"m_kT_mWOSYh_eK3kF8chtyCWfcD9-gomvu4mhaMFQl-gt4D3LjORi6k3BXD6_x9a-FOUt-D2LFuywJN6HfrpAnDg\",\"text\":\"asdfa\"}}]}]}";
  public static final String SAMPLE_IG_MESSAGE_ECHO =
      "{\"object\":\"instagram\",\"entry\":[{\"time\":1700539232059,\"id\":\"90010138419114\",\"messaging\":[{\"sender\":{\"id\":\"90010138419114\"},\"recipient\":{\"id\":\"7069898263033409\"},\"timestamp\":1700539231795,\"message\":{\"mid\":\"aWdfZAG1faXRlbToxOklHTWVzc2FnZAUlEOjkwMDEwMTM4NDE5MTE0OjM0MDI4MjM2Njg0MTcxMDMwMTI0NDI1OTQ1NTA3ODk2MDQ3NzQ1MDozMTM2OTQxMTk5NjIzMzQ1ODM2OTgxMjI5OTgwNTM2MDEyOAZDZD\",\"text\":\"This is a reply\",\"is_echo\":true}}]}]}";

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String SAMPLE_MESSAGE_HMAC =
      "sha256=8620d18213fa2612d16117b65168ef97404fa13189528014c5362fec31215985";
  public static final JsonNode PARSED_SAMPLE_MESSAGE;

  static {
    try {
      PARSED_SAMPLE_MESSAGE = MAPPER.readTree(SAMPLE_MESSAGE);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private Javalin app;
  private BlockingQueue<OutboundRequest> requests;

  private static HttpResponse getRequest(String path, int port, Map<String, String> params)
      throws IOException, URISyntaxException {
    URIBuilder uriBuilder =
        URIBuilder.loopbackAddress().setScheme("http").setPort(port).appendPath(path);
    params.forEach(uriBuilder::addParameter);
    return Request.get(uriBuilder.build()).execute().returnResponse();
  }

  private static Request createMessageRequest(
      String body, ServicesRunner runner, boolean calculateHmac) throws URISyntaxException {
    @SuppressWarnings("unchecked") // for the scope of this test this is guaranteed
    Service<FBMessage> service =
        (Service<FBMessage>) runner.services().stream().findAny().orElseThrow();
    String path = service.path();

    // for the scope of this test this is guaranteed
    FBMessageHandler messageHandler = (FBMessageHandler) service.messageHandler();

    URI uri =
        URIBuilder.loopbackAddress()
            .setScheme("http")
            .setPort(runner.port())
            .appendPath(path)
            .build();

    Request request =
        Request.post(uri)
            .bodyString(body, ContentType.APPLICATION_JSON)
            .setHeader("Content-Type", "application/json");
    if (calculateHmac) {
      String hmac = messageHandler.hmac(body);
      request.setHeader("X-Hub-Signature-256", "sha256=" + hmac);
    }

    return request;
  }

  public static Request createMessageRequest(String body, ServicesRunner runner)
      throws URISyntaxException {
    return createMessageRequest(body, runner, true);
  }

  static Stream<Arguments> requestFactory() throws JsonProcessingException {
    JsonNode duplicateMessage = MAPPER.readTree(SAMPLE_MESSAGE);
    ArrayNode messagingArray = (ArrayNode) duplicateMessage.get("entry").get(0).get("messaging");
    messagingArray.add(messagingArray.get(0));

    JsonNode duplicateEntry = MAPPER.readTree(SAMPLE_MESSAGE);
    ArrayNode entryArray = (ArrayNode) duplicateMessage.get("entry");
    entryArray.add(entryArray.get(0));
    Stream<TestArgument> arguments =
        Stream.of(
            new TestArgument(
                "valid sample",
                200,
                r ->
                    createMessageRequest(SAMPLE_MESSAGE, r, false)
                        .addHeader("X-Hub-Signature-256", SAMPLE_MESSAGE_HMAC),
                true),
            new TestArgument(
                // if this fails the test hmac calculator is flawed
                "valid sample with hmac calculation",
                200,
                r -> createMessageRequest(SAMPLE_MESSAGE, r),
                true),
            new TestArgument(
                // if this fails the test hmac calculator is flawed
                "test the send it twice scenario",
                200,
                r -> createMessageRequest(SAMPLE_MESSAGE, r),
                true,
                2),
            new TestArgument(
                "valid instagram message",
                200,
                r -> createMessageRequest(SAMPLE_IG_MESSAGE, r),
                true,
                true),
            new TestArgument(
                "instagram message echo",
                200,
                r -> createMessageRequest(SAMPLE_IG_MESSAGE_ECHO, r),
                false,
                true),
            new TestArgument(
                "a non page object type",
                400,
                r -> createMessageRequest("{\"object\": \"not a page\"}", r),
                false),
            new TestArgument(
                "an invalid page object",
                400,
                r -> createMessageRequest("{\"object\": \"page\"}", r),
                false),
            new TestArgument(
                "valid body with no entries/messages",
                200,
                r -> createMessageRequest("{\"object\": \"page\", \"entry\": []}", r),
                false),
            new TestArgument(
                "unknown entry objects",
                200,
                r -> createMessageRequest("{\"object\": \"page\", \"entry\": [{}]}", r),
                false),
            new TestArgument(
                "valid body without any messaging objects",
                200,
                r ->
                    createMessageRequest(
                        "{\"object\": \"page\", \"entry\": [{\"messaging\": []}]}", r),
                false),
            new TestArgument(
                "valid body with duplicate message",
                200,
                r -> createMessageRequest(MAPPER.writeValueAsString(duplicateMessage), r),
                true),
            new TestArgument(
                "valid body with duplicate entry",
                200,
                r -> createMessageRequest(MAPPER.writeValueAsString(duplicateEntry), r),
                true),
            new TestArgument(
                "malformed messaging object",
                400,
                r ->
                    createMessageRequest(
                        "{\"object\": \"page\", \"entry\": [{\"messaging\": [{}]}]}", r),
                false),
            new TestArgument(
                "messaging object with no messages",
                200,
                r ->
                    createMessageRequest(
                        "{\"object\": \"page\", \"entry\": [{\"messaging\": [{\"recipient\": {\"id\": 123}, \"sender\": {\"id\": 123}, \"timestamp\": 0}]}]}",
                        r),
                false),
            new TestArgument(
                "invalid message object",
                400,
                r ->
                    createMessageRequest(
                        "{\"object\": \"page\", \"entry\": [{\"messaging\": [{\"recipient\": {\"id\": 123}, \"sender\": {\"id\": 123}, \"timestamp\": 0, \"message\": {}}]}]}",
                        r),
                false),
            new TestArgument(
                "message object without text",
                200,
                r ->
                    createMessageRequest(
                        "{\"object\": \"page\", \"entry\": [{\"messaging\": [{\"recipient\": {\"id\": 123}, \"sender\": {\"id\": 123}, \"timestamp\": 0, \"message\": {\"mid\": \"abc123789\"}}]}]}",
                        r),
                false),
            new TestArgument(
                "missing hmac", 400, r -> createMessageRequest(SAMPLE_MESSAGE, r, false), false),
            new TestArgument(
                "invalid json", 400, r -> createMessageRequest("invalid_json.", r), false),
            new TestArgument(
                "valid json, invalid body", 400, r -> createMessageRequest("{}", r), false),
            new TestArgument(
                "invalid hmac",
                400,
                r ->
                    createMessageRequest(SAMPLE_MESSAGE, r, false)
                        .addHeader("X-Hub-Signature-256", "abcdef0123456789"),
                false));

    return arguments.map(
        a ->
            Arguments.of(
                Named.of(a.name, a.expectedReturnCode),
                Named.of("_", a.requestFactory),
                Named.of("_", a.messageExpected),
                Named.of("_", a.timesToSendMessage()),
                Named.of("_", a.isInstagram())));
  }

  @BeforeEach
  void setUp() {

    app = Javalin.create();
    app.addHttpHandler(
        HandlerType.GET, "*", ctx -> fail("the pipeline should only be sending post requests"));
    app.addHttpHandler(
        HandlerType.DELETE, "*", ctx -> fail("the pipeline should only be sending post requests"));
    app.addHttpHandler(
        HandlerType.PUT, "*", ctx -> fail("the pipeline should only be sending post requests"));
    requests = new LinkedBlockingQueue<>();
    app.addHttpHandler(
        HandlerType.POST,
        "/",
        ctx -> requests.add(new OutboundRequest(ctx.body(), ctx.headerMap(), ctx.queryParamMap())));
  }

  @AfterEach
  void tearDown() {
    app.stop();
  }

  @Test
  void validation() throws IOException, URISyntaxException {
    final String pageToken = "243af3c6-9994-4869-ae13-ad61a38323f5"; // this is fake
    final int challenge = 1158201444;
    final String verifyToken = "oamnw9230rjadoia";
    Service<FBMessage> service =
        new Service<>(
            MemoryStoreConfig.of(1, 1).toStore(),
            new FBMessageHandler(verifyToken, pageToken, "dummy"),
            new DummyPlugin<>("this is a dummy message"),
            "/testfbmessage");
    final ServicesRunner runner = ServicesRunner.newInstance().service(service).port(0);
    HttpResponse response;
    try (ServicesRunner ignored = runner.start()) {
      ImmutableMap<String, String> params =
          ImmutableMap.<String, String>builder()
              .put("hub.mode", "subscribe")
              .put("hub.challenge", Integer.toString(challenge))
              .put("hub.verify_token", verifyToken)
              .build();
      response = getRequest("testfbmessage", runner.port(), params);
    }
    assertThat(response.getCode()).isEqualTo(200);
    InputStream inputStream = ((BasicClassicHttpResponse) response).getEntity().getContent();
    byte[] input = new byte[inputStream.available()];
    inputStream.read(input);
    String text = new String(input, StandardCharsets.UTF_8);
    assertThat(text).isEqualTo(Integer.toString(challenge));
  }

  private Function<Identifier, URI> testURLFactoryFactory(Identifier pageId) {
    return p -> {
      Preconditions.checkArgument(p.equals(pageId));
      try {
        return URIBuilder.loopbackAddress().setScheme("http").setPort(app.port()).build();
      } catch (URISyntaxException e) {
        fail("failed building url");
        throw new RuntimeException(e);
      }
    };
  }

  @Test
  void nonTextPluginResponse() throws IOException, URISyntaxException, InterruptedException {
    String path = "/testfbmessage";
    Identifier pageId = Identifier.from(106195825075770L);
    String token = "243af3c6-9994-4869-ae13-ad61a38323f5"; // this is fake don't worry
    String secret = "f74a638462f975e9eadfcbb84e4aa06b"; // it's been rolled don't worry

    FBMessageHandler messageHandler = new FBMessageHandler("0", token, secret);
    DummyPlugin<FBMessage> llmHandler =
        new DummyPlugin<>(
            List.of(new Payload.Document(new byte[0], "application/pdf")),
            "this is a dummy message");
    MemoryStore<FBMessage> memoryStore = MemoryStoreConfig.of(1, 1).toStore();
    Service<FBMessage> service = new Service<>(memoryStore, messageHandler, llmHandler, path);
    final ServicesRunner runner = ServicesRunner.newInstance().service(service).port(0);

    app.start(0);
    runner.start();
    messageHandler.baseURLFactory(testURLFactoryFactory(pageId));
    createMessageRequest(SAMPLE_MESSAGE, runner).execute();
    ObjectNode m = (ObjectNode) MAPPER.readTree(SAMPLE_MESSAGE);
    ObjectNode messageObject =
        (ObjectNode) m.get("entry").get(0).get("messaging").get(0).get("message");
    messageObject.put("mid", "notarealmid");

    createMessageRequest(MAPPER.writeValueAsString(m), runner).execute();
    @Nullable OutboundRequest r = requests.poll(500, TimeUnit.MILLISECONDS);
    assertThat(r).isNotNull();
  }

  @ParameterizedTest
  @MethodSource("requestFactory")
  void invalidMessage(
      int expectedReturnCode,
      ThrowableFunction<ServicesRunner, Request> requestFactory,
      boolean messageExpected,
      int timesToSendMessage,
      boolean isInstagram)
      throws Exception {
    String path = "/testfbmessage";
    Identifier pageId = Identifier.from(106195825075770L);
    String token = "243af3c6-9994-4869-ae13-ad61a38323f5"; // this is fake don't worry
    String secret = "f74a638462f975e9eadfcbb84e4aa06b"; // it's been rolled don't worry

    FBMessageHandler messageHandler;
    if (isInstagram) {
      messageHandler = new FBMessageHandler("0", token, secret, pageId.toString());
    } else {
      messageHandler = new FBMessageHandler("0", token, secret);
    }
    DummyPlugin<FBMessage> llmHandler = new DummyPlugin<>("this is a dummy message");
    MemoryStore<FBMessage> memoryStore = MemoryStoreConfig.of(1, 1).toStore();
    Service<FBMessage> service = new Service<>(memoryStore, messageHandler, llmHandler, path);
    final ServicesRunner runner = ServicesRunner.newInstance().service(service).port(0);

    app.start(0);
    runner.start();
    messageHandler.baseURLFactory(testURLFactoryFactory(pageId));

    @Nullable Response response = null;
    for (int ignored = 0; ignored < timesToSendMessage; ignored++) {
      Request request = requestFactory.apply(runner);
      response = request.execute();
    }
    assert response != null;
    assertThat(response.returnResponse().getCode()).isEqualTo(expectedReturnCode);
    if (!messageExpected) {
      assertThat(llmHandler.poll())
          .isNull(); // make sure the message wasn't processed and send to the llm handler
      assertThat(memoryStore.size())
          .isEqualTo(0); // make sure the message wasn't processed and stored
      assertThat(requests).hasSize(0);
    } else {
      ThreadState<FBMessage> thread = llmHandler.take(500);
      JsonNode messageObject = PARSED_SAMPLE_MESSAGE.get("entry").get(0).get("messaging").get(0);
      String messageText = messageObject.get("message").get("text").textValue();
      String mid = messageObject.get("message").get("mid").textValue();
      Identifier recipientId =
          Identifier.from(messageObject.get("recipient").get("id").textValue());
      Identifier senderId = Identifier.from(messageObject.get("sender").get("id").textValue());
      Instant timestamp = Instant.ofEpochMilli(messageObject.get("timestamp").longValue());
      assertThat(thread.messages())
          .hasSize(1)
          .allSatisfy(m -> assertThat(m.message()).isEqualTo(messageText))
          .allSatisfy(m -> assertThat(m.instanceId().toString()).isEqualTo(mid))
          .allSatisfy(m -> assertThat(m.role()).isSameAs(Role.USER))
          .allSatisfy(m -> assertThat(m.timestamp()).isEqualTo(timestamp))
          .allSatisfy(m -> assertThat(m.recipientId()).isEqualTo(recipientId))
          .allSatisfy(m -> assertThat(m.senderId()).isEqualTo(senderId));

      @Nullable OutboundRequest r = requests.poll(500, TimeUnit.MILLISECONDS);
      assertThat(r).isNotNull();
      assertThat(r.queryParamMap().get("access_token"))
          .hasSize(1)
          .allSatisfy(t -> assertThat(t).isEqualTo(token));
      JsonNode body = MAPPER.readTree(r.body);
      assertThat(body.get("messaging_type").textValue()).isEqualTo("RESPONSE");
      assertThat(body.get("recipient").get("id").textValue()).isEqualTo(senderId.toString());
      assertThat(body.get("message").get("text").textValue()).isEqualTo(llmHandler.dummyResponse());
    }
  }

  @Test
  void chunkingHappens() throws IOException {
    app.start(0);
    Identifier pageId = Identifier.from(106195825075770L);
    String token = "243af3c6-9994-4869-ae13-ad61a38323f5"; // this is fake don't worry
    String secret = "f74a638462f975e9eadfcbb84e4aa06b"; // it's been rolled don't worry
    FBMessageHandler messageHandler =
        new FBMessageHandler("0", token, secret).baseURLFactory(testURLFactoryFactory(pageId));

    String bigText =
        Stream.generate(() -> "0123456789.").limit(300).collect(Collectors.joining(" "));
    FBMessage bigMessage =
        new FBMessage(
            Instant.now(), Identifier.random(), pageId, Identifier.random(), bigText, Role.USER);
    messageHandler.respond(bigMessage);
    assertThat(requests.size()).isEqualTo(300);
    assertThat(requests).allSatisfy(m -> assertThat(m.body()).contains("0123456789"));
  }

  @FunctionalInterface
  private interface ThrowableFunction<T, R> {
    R apply(T in) throws Exception;
  }

  private record OutboundRequest(
      String body, Map<String, String> headerMap, Map<String, List<String>> queryParamMap) {}

  private record TestArgument(
      String name,
      int expectedReturnCode,
      ThrowableFunction<ServicesRunner, Request> requestFactory,
      boolean messageExpected,
      int timesToSendMessage,
      boolean isInstagram) {

    private TestArgument(
        String name,
        int expectedReturnCode,
        ThrowableFunction<ServicesRunner, Request> requestFactory,
        boolean messageExpected,
        boolean isInstagram) {
      this(name, expectedReturnCode, requestFactory, messageExpected, 1, isInstagram);
    }

    private TestArgument(
        String name,
        int expectedReturnCode,
        ThrowableFunction<ServicesRunner, Request> requestFactory,
        boolean messageExpected,
        int timesToSendMessage) {
      this(name, expectedReturnCode, requestFactory, messageExpected, timesToSendMessage, false);
    }

    private TestArgument(
        String name,
        int expectedReturnCode,
        ThrowableFunction<ServicesRunner, Request> requestFactory,
        boolean messageExpected) {
      this(name, expectedReturnCode, requestFactory, messageExpected, 1);
    }
  }
}
