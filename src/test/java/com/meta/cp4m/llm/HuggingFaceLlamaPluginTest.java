/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.Service;
import com.meta.cp4m.ServicesRunner;
import com.meta.cp4m.configuration.ConfigurationUtils;
import com.meta.cp4m.message.*;
import com.meta.cp4m.message.Message.Role;
import com.meta.cp4m.store.ChatStore;
import com.meta.cp4m.store.MemoryStoreConfig;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class HuggingFaceLlamaPluginTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  public static final ArrayNode SAMPLE_RESPONSE = MAPPER.createArrayNode();
  private static final String PATH = "/";
  private static final String TEST_MESSAGE = "this is a test message";
  private static final String TEST_SYSTEM_MESSAGE = "this is a system message";
  private static final String TEST_PAYLOAD = "<s>[INST] test message [/INST]";
  private static final String TEST_PAYLOAD_WITH_SYSTEM =
      "<s>[INST] <<SYS>>\nthis is a system message\n<</SYS>>\n\nthis is a test message [/INST]";

  private static final ThreadState<FBMessage> STACK =
      ThreadState.of(
          MessageFactory.instance(FBMessage.class)
              .newMessage(
                  Instant.now(),
                  "test message",
                  Identifier.random(),
                  Identifier.random(),
                  Identifier.random(),
                  Role.USER));

  static {
    SAMPLE_RESPONSE.addObject().put("generated_text", TEST_MESSAGE);
  }

  private BlockingQueue<OutboundRequest> HuggingFaceLlamaRequests;
  private Javalin app;
  private URI endpoint;
  private ObjectNode minimalConfig;

  static Stream<HuggingFaceConfigTest.ConfigItem> modelOptions() {
    Set<String> non_model_options = Set.of("name", "type", "api_key", "max_input_tokens");
    return HuggingFaceConfigTest.CONFIG_ITEMS.stream()
        .filter(c -> !non_model_options.contains(c.key()));
  }

  @BeforeEach
  void setUp() throws UnknownHostException, URISyntaxException {
    HuggingFaceLlamaRequests = new LinkedBlockingDeque<>();
    app = Javalin.create();
    app.before(
        PATH,
        ctx ->
            HuggingFaceLlamaRequests.add(
                new OutboundRequest(ctx.body(), ctx.headerMap(), ctx.queryParamMap())));
    app.post(PATH, ctx -> ctx.result(MAPPER.writeValueAsString(SAMPLE_RESPONSE)));
    app.start(0);
    endpoint =
        URIBuilder.localhost().setScheme("http").appendPath(PATH).setPort(app.port()).build();
  }

  @Test
  void sampleValid() throws IOException, InterruptedException {
    String apiKey = UUID.randomUUID().toString();
    HuggingFaceConfig config =
        HuggingFaceConfig.builder(apiKey).endpoint(endpoint.toString()).tokenLimit(100).build();
    HuggingFaceLlamaPlugin<FBMessage> plugin = new HuggingFaceLlamaPlugin<>(config);
    FBMessage message = plugin.handle(STACK);
    assertThat(message.message()).isEqualTo(TEST_MESSAGE);
    assertThat(message.role()).isSameAs(Role.ASSISTANT);
    assertThatCode(() -> STACK.with(message)).doesNotThrowAnyException();
    @Nullable OutboundRequest or = HuggingFaceLlamaRequests.poll(500, TimeUnit.MILLISECONDS);
    assertThat(or).isNotNull();
    assertThat(or.headerMap().get("Authorization")).isNotNull().isEqualTo("Bearer " + apiKey);
  }

  @Test
  void createPayload() {
    String apiKey = UUID.randomUUID().toString();
    HuggingFaceConfig config =
        HuggingFaceConfig.builder(apiKey).endpoint(endpoint.toString()).tokenLimit(100).build();
    HuggingFaceLlamaPlugin<FBMessage> plugin = new HuggingFaceLlamaPlugin<>(config);
    HuggingFaceLlamaPromptBuilder<FBMessage> promptBuilder = new HuggingFaceLlamaPromptBuilder<>();
    String createdPayload = promptBuilder.createPrompt(STACK, config);
    assertThat(createdPayload).isEqualTo(TEST_PAYLOAD);
  }

  @Test
  void createPayloadWithSystemMessage() {
    String apiKey = UUID.randomUUID().toString();
    HuggingFaceConfig config =
        HuggingFaceConfig.builder(apiKey).endpoint(endpoint.toString()).tokenLimit(100).systemMessage(TEST_SYSTEM_MESSAGE).build();
    HuggingFaceLlamaPlugin<FBMessage> plugin = new HuggingFaceLlamaPlugin<>(config);
    ThreadState<FBMessage> stack =
        ThreadState.of(
            MessageFactory.instance(FBMessage.class)
                .newMessage(
                    Instant.now(),
                        TEST_MESSAGE,
                    Identifier.random(),
                    Identifier.random(),
                    Identifier.random(),
                    Role.USER));
    HuggingFaceLlamaPromptBuilder<FBMessage> promptBuilder = new HuggingFaceLlamaPromptBuilder<>();
    String createdPayload = promptBuilder.createPrompt(stack, config);
    assertThat(createdPayload).isEqualTo(TEST_PAYLOAD_WITH_SYSTEM);
  }

  @Test
  void createPayloadWithConfigSystemMessage() {
    String apiKey = UUID.randomUUID().toString();
    HuggingFaceConfig config =
        HuggingFaceConfig.builder(apiKey)
            .endpoint(endpoint.toString())
            .tokenLimit(100)
            .systemMessage(TEST_SYSTEM_MESSAGE)
            .build();
    HuggingFaceLlamaPlugin<FBMessage> plugin = new HuggingFaceLlamaPlugin<>(config);
    ThreadState<FBMessage> stack =
        ThreadState.of(
            MessageFactory.instance(FBMessage.class)
                .newMessage(
                    Instant.now(),
                    TEST_MESSAGE,
                    Identifier.random(),
                    Identifier.random(),
                    Identifier.random(),
                    Role.USER));
    HuggingFaceLlamaPromptBuilder<FBMessage> promptBuilder = new HuggingFaceLlamaPromptBuilder<>();
    String createdPayload = promptBuilder.createPrompt(stack, config);
    assertThat(createdPayload).isEqualTo(TEST_PAYLOAD_WITH_SYSTEM);
  }

  @Test
  void contextTooBig() throws IOException {
    String apiKey = UUID.randomUUID().toString();
    HuggingFaceConfig config =
            HuggingFaceConfig.builder(apiKey)
                    .endpoint(endpoint.toString())
                    .tokenLimit(200)
                    .maxInputTokens(100)
                    .systemMessage(TEST_SYSTEM_MESSAGE)
                    .build();
    HuggingFaceLlamaPlugin<FBMessage> plugin = new HuggingFaceLlamaPlugin<>(config);
    ThreadState<FBMessage> thread =
            ThreadState.of(
                    MessageFactory.instance(FBMessage.class)
                            .newMessage(
                                    Instant.now(),
                                    Stream.generate(() -> "0123456789").limit(100).collect(Collectors.joining()),
                                    Identifier.random(),
                                    Identifier.random(),
                                    Identifier.random(),
                                    Role.USER));
    FBMessage response = plugin.handle(thread);
    assertThat(response.message()).isEqualTo("I'm sorry but that request was too long for me.");
  }

  @Test
  void truncatesContext() throws IOException {
    String apiKey = UUID.randomUUID().toString();
    HuggingFaceConfig config =
            HuggingFaceConfig.builder(apiKey)
                    .endpoint(endpoint.toString())
                    .tokenLimit(200)
                    .maxInputTokens(100)
                    .build();
    HuggingFaceLlamaPlugin<FBMessage> plugin = new HuggingFaceLlamaPlugin<>(config);
    ThreadState<FBMessage> thread =
            ThreadState.of(
                    MessageFactory.instance(FBMessage.class)
                            .newMessage(
                                    Instant.now(),
                                     Stream.generate(() -> "0123456789").limit(100).collect(Collectors.joining()),
                                    Identifier.random(),
                                    Identifier.random(),
                                    Identifier.random(),
                                    Role.USER));
    thread = thread.with(thread.newMessageFromUser(Instant.now(), "test message", Identifier.from(2)));
    HuggingFaceLlamaPromptBuilder<FBMessage> promptBuilder = new HuggingFaceLlamaPromptBuilder<>();
    String createdPayload = promptBuilder.createPrompt(thread, config);
    assertThat(createdPayload).isEqualTo(TEST_PAYLOAD);
  }

  @BeforeEach
  void setUpMinConfig() {
    minimalConfig = MAPPER.createObjectNode();
    HuggingFaceConfigTest.CONFIG_ITEMS.forEach(
        t -> {
          if (t.required()) {
            minimalConfig.set(t.key(), t.validValue());
          }
        });
  }

  @ParameterizedTest
  @MethodSource("modelOptions")
  void validConfigValues(HuggingFaceConfigTest.ConfigItem configItem)
      throws IOException, InterruptedException {
    minimalConfig.set(configItem.key(), configItem.validValue());
    minimalConfig.put("endpoint", endpoint.toString()); // needs the correct endpoint to run
    HuggingFaceConfig config =
        ConfigurationUtils.jsonMapper().convertValue(minimalConfig, HuggingFaceConfig.class);
    HuggingFaceLlamaPlugin<FBMessage> plugin = new HuggingFaceLlamaPlugin<>(config);
    FBMessage message = plugin.handle(STACK);
    assertThat(message.message()).isEqualTo(TEST_MESSAGE);
    assertThat(message.role()).isSameAs(Role.ASSISTANT);
    assertThatCode(() -> STACK.with(message)).doesNotThrowAnyException();
    @Nullable OutboundRequest or = HuggingFaceLlamaRequests.poll(500, TimeUnit.MILLISECONDS);
    assertThat(or).isNotNull();
    System.out.println(or);
    assertThat(or.headerMap().get("Authorization"))
        .isNotNull()
        .isEqualTo("Bearer " + config.apiKey());
  }

  @Test
  void orderedCorrectly() throws IOException, InterruptedException {
    HuggingFaceConfig config =
        HuggingFaceConfig.builder("lkjasdlkjasdf")
            .maxInputTokens(100)
            .tokenLimit(200)
            .endpoint(endpoint.toString())
            .build();
    HuggingFaceLlamaPlugin<FBMessage> plugin = new HuggingFaceLlamaPlugin<>(config);
    ThreadState<FBMessage> stack =
        ThreadState.of(
            MessageFactory.instance(FBMessage.class)
                .newMessage(
                    Instant.now(),
                    "1",
                    Identifier.random(),
                    Identifier.random(),
                    Identifier.random(),
                    Role.SYSTEM));
    stack = stack.with(stack.newMessageFromUser(Instant.now(), "2", Identifier.from(2)));
    stack = stack.with(stack.newMessageFromUser(Instant.now(), "3", Identifier.from(3)));
    stack = stack.with(stack.newMessageFromUser(Instant.now(), "4", Identifier.from(4)));
    plugin.handle(stack);
    @Nullable OutboundRequest or = HuggingFaceLlamaRequests.poll(500, TimeUnit.MILLISECONDS);
    assertThat(or).isNotNull();
    JsonNode body = MAPPER.readTree(or.body());

    int prevMessageIndex = 0;
    for (int i = 0; i < stack.messages().size(); i++) {
      FBMessage stackMessage = stack.messages().get(i);
      String sentMessage = body.get("inputs").textValue();
      int index = sentMessage.indexOf(stackMessage.message());
      int finalPrevMessageIndex = prevMessageIndex;
      assertSoftly(s -> s.assertThat(index).isGreaterThan(finalPrevMessageIndex));
      prevMessageIndex = index;
    }
  }

  @Test
  void inPipeline() throws IOException, URISyntaxException, InterruptedException {
    ChatStore<FBMessage> store = MemoryStoreConfig.of(1, 1).toStore();
    String appSecret = "app secret";
    String accessToken = "access token";
    String verifyToken = "verify token";

    BlockingQueue<OutboundRequest> metaRequests = new LinkedBlockingDeque<>();
    String metaPath = "/meta";
    URI messageReceiver =
        URIBuilder.localhost().appendPath(metaPath).setScheme("http").setPort(app.port()).build();
    app.post(
        metaPath,
        ctx ->
            metaRequests.put(
                new OutboundRequest(ctx.body(), ctx.headerMap(), ctx.queryParamMap())));
    FBMessageHandler handler =
        new FBMessageHandler(verifyToken, accessToken, appSecret)
            .baseURLFactory(ignored -> messageReceiver);

    String apiKey = "api key";
    HuggingFaceConfig config =
        HuggingFaceConfig.builder(apiKey).endpoint(endpoint.toString()).tokenLimit(1000).build();
    HuggingFaceLlamaPlugin<FBMessage> plugin = new HuggingFaceLlamaPlugin<>(config);

    String webhookPath = "/webhook";
    Service<FBMessage> service = new Service<>(store, handler, plugin, webhookPath);
    ServicesRunner runner = ServicesRunner.newInstance().service(service).port(0);
    runner.start();

    // TODO: create test harness
    Request request =
        FBMessageHandlerTest.createMessageRequest(FBMessageHandlerTest.SAMPLE_MESSAGE, runner);
    HttpResponse response = request.execute().returnResponse();
    assertThat(response.getCode()).isEqualTo(200);
    @Nullable OutboundRequest or = HuggingFaceLlamaRequests.poll(500, TimeUnit.MILLISECONDS);
    assertThat(or).isNotNull();
    assertThat(or.headerMap().get("Authorization"))
        .isNotNull()
        .isEqualTo("Bearer " + config.apiKey());
    JsonNode body = ConfigurationUtils.jsonMapper().readTree(or.body());

    or = metaRequests.poll(500, TimeUnit.MILLISECONDS);
    // plugin output got back to meta
    assertThat(or).isNotNull().satisfies(r -> assertThat(r.body()).contains(TEST_MESSAGE));
  }

  private record OutboundRequest(
      String body, Map<String, String> headerMap, Map<String, List<String>> queryParamMap) {}
}
