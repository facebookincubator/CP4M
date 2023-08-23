/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.meta.chatbridge.FBID;
import com.meta.chatbridge.Pipeline;
import com.meta.chatbridge.PipelinesRunner;
import com.meta.chatbridge.llm.DummyFBMessageLLMHandler;
import com.meta.chatbridge.message.Message.Role;
import com.meta.chatbridge.store.MemoryStore;
import com.meta.chatbridge.store.MessageStack;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FBMessageHandlerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Example message collected directly from the messenger webhook */
  private static final String SAMPLE_MESSAGE =
      "{\"object\":\"page\",\"entry\":[{\"id\":\"106195825075770\",\"time\":1692813219204,\"messaging\":[{\"sender\":{\"id\":\"6357858494326947\"},\"recipient\":{\"id\":\"106195825075770\"},\"timestamp\":1692813218705,\"message\":{\"mid\":\"m_kT_mWOSYh_eK3kF8chtyCWfcD9-gomvu4mhaMFQl-gt4D3LjORi6k3BXD6_x9a-FOUt-D2LFuywJN6HfrpAnDg\",\"text\":\"asdfa\"}}]}]}";

  private static final String SAMPLE_MESSAGE_HMAC =
      "sha256=8620d18213fa2612d16117b65168ef97404fa13189528014c5362fec31215985";

  private static JsonNode PARSED_SAMPLE_MESSAGE;

  @BeforeAll
  static void beforeAll() throws JsonProcessingException {
    PARSED_SAMPLE_MESSAGE = MAPPER.readTree(SAMPLE_MESSAGE);
  }

  private Javalin app;

  private HttpResponse getRequest(String path, int port, Map<String, String> params)
      throws IOException, URISyntaxException {
    URIBuilder uriBuilder =
        URIBuilder.loopbackAddress().setScheme("http").setPort(port).appendPath(path);
    params.forEach(uriBuilder::addParameter);
    return Request.get(uriBuilder.build()).execute().returnResponse();
  }

  @BeforeEach
  void setUp() {
    app = Javalin.create();
  }

  @AfterEach
  void tearDown() {
    app.close();
  }

  @Test
  void validation() throws IOException, URISyntaxException {
    String token = "243af3c6-9994-4869-ae13-ad61a38323f5"; // this is fake
    int challenge = 1158201444;
    Pipeline<FBMessage> pipeline =
        new Pipeline<>(
            new MemoryStore<>(),
            new FBMessageHandler("0", token, "dummy"),
            new DummyFBMessageLLMHandler("this is a dummy message"),
            "/testfbmessage");
    final PipelinesRunner runner = PipelinesRunner.newInstance().pipeline(pipeline).port(0);
    HttpResponse response;
    try (PipelinesRunner ignored = runner.start()) {
      ImmutableMap<String, String> params =
          ImmutableMap.<String, String>builder()
              .put("hub.mode", "subscribe")
              .put("hub.challenge", Integer.toString(challenge))
              .put("hub.verify_token", token)
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

  private Request createMessageRequest(String body, PipelinesRunner runner)
      throws IOException, URISyntaxException {
    String path = runner.pipelines().stream().findAny().get().path();
    URI uri =
        URIBuilder.localhost().setScheme("http").setPort(runner.port()).appendPath(path).build();

    return Request.post(uri).bodyString(body, ContentType.APPLICATION_JSON);
  }

  @Test
  void messageInbound() throws IOException, URISyntaxException, InterruptedException {
    String path = "/testfbmessage";
    FBID pageId = FBID.from(106195825075770L);
    String token = "243af3c6-9994-4869-ae13-ad61a38323f5";
    String secret = "f74a638462f975e9eadfcbb84e4aa06b"; // it's been rolled don't worry
    FBMessageHandler messageHandler = new FBMessageHandler("0", token, secret);
    DummyFBMessageLLMHandler llmHandler = new DummyFBMessageLLMHandler("this is a dummy message");
    Pipeline<FBMessage> pipeline =
        new Pipeline<>(new MemoryStore<>(), messageHandler, llmHandler, path);
    final PipelinesRunner runner = PipelinesRunner.newInstance().pipeline(pipeline).port(0);

    app.addHandler(
        HandlerType.GET, "*", ctx -> fail("the pipeline should only be sending post requests"));
    app.addHandler(
        HandlerType.DELETE, "*", ctx -> fail("the pipeline should only be sending post requests"));
    app.addHandler(
        HandlerType.PUT, "*", ctx -> fail("the pipeline should only be sending post requests"));

    record OutboundRequest(
        String body, Map<String, String> headerMap, Map<String, List<String>> queryParamMap) {}
    BlockingQueue<OutboundRequest> requests = new LinkedBlockingQueue<>();
    app.addHandler(
        HandlerType.POST,
        "/",
        ctx -> requests.add(new OutboundRequest(ctx.body(), ctx.headerMap(), ctx.queryParamMap())));
    app.start(0);
    runner.start();

    messageHandler.baseURLFactory(
        p -> {
          assertThat(p).isEqualTo(pageId);
          try {
            return URIBuilder.localhost().setScheme("http").setPort(app.port()).build();
          } catch (URISyntaxException | UnknownHostException e) {
            fail("failed building url");
            throw new RuntimeException(e);
          }
        });
    //    ObjectNode body = MAPPER.createObjectNode();
    //        String mid = "1234";
    //        String messageText = "test message";
    //    body.put("object", "page").put("id", pageId.toString());
    //    ObjectNode messageObject =
    // body.putArray("entry").addObject().putArray("messaging").addObject();
    //    messageObject.putObject("sender").put("id", "123");
    //    messageObject.putObject("recipient").put("id", pageId.toString());
    //    messageObject.put("timestamp", Instant.now().toEpochMilli());
    //    messageObject.putObject("message").put("mid", mid).put("text", messageText);

    Response response =
        createMessageRequest(SAMPLE_MESSAGE, runner)
            .addHeader("X-Hub-Signature-256", SAMPLE_MESSAGE_HMAC)
            .execute();
    assertThat(response.returnResponse().getCode()).isEqualTo(200);
    MessageStack<FBMessage> stack = llmHandler.take(500);
    JsonNode messageObject = PARSED_SAMPLE_MESSAGE.get("entry").get(0).get("messaging").get(0);
    String messageText = messageObject.get("message").get("text").textValue();
    String mid = messageObject.get("message").get("mid").textValue();
    FBID recipientId = FBID.from(messageObject.get("recipient").get("id").textValue());
    FBID senderId = FBID.from(messageObject.get("sender").get("id").textValue());
    Instant timestamp = Instant.ofEpochMilli(messageObject.get("timestamp").longValue());
    assertThat(stack.messages())
        .hasSize(1)
        .allSatisfy(m -> assertThat(m.message()).isEqualTo(messageText))
        .allSatisfy(m -> assertThat(m.instanceId()).isEqualTo(mid))
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
    assertThat(body.get("recipient").get("id").asLong()).isEqualTo(senderId.longValue());
    assertThat(body.get("message").get("text").textValue()).isEqualTo(llmHandler.dummyResponse());
  }
}
