/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

public class OpenAIPluginTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  public static final JsonNode SAMPLE_RESPONSE = MAPPER.createObjectNode();
  private static final String PATH = "/";
  private static final String TEST_MESSAGE = "this is a test message";
  private static final ThreadState<FBMessage> THREAD =
      ThreadState.of(
          MessageFactory.instance(FBMessage.class)
              .newMessage(
                  Instant.now(),
                  new Payload.Text("test message"),
                  Identifier.random(),
                  Identifier.random(),
                  Identifier.random(),
                  Role.USER));

  static {
    ((ObjectNode) SAMPLE_RESPONSE)
        .put("created", Instant.now().getEpochSecond())
        .put("object", "chat.completion")
        .put("id", UUID.randomUUID().toString())
        .putArray("choices")
        .addObject()
        .put("index", 0)
        .put("finish_reason", "stop")
        .putObject("message")
        .put("role", "assistant")
        .put("content", TEST_MESSAGE);
  }

  private BlockingQueue<OutboundRequest> openAIRequests;
  private Javalin app;
  private URI endpoint;
  private ObjectNode minimalConfig;

  static Stream<OpenAIConfigTest.ConfigItem> modelOptions() {
    Set<String> non_model_options = Set.of("name", "type", "model", "api_key", "max_input_tokens");
    return OpenAIConfigTest.CONFIG_ITEMS.stream().filter(c -> !non_model_options.contains(c.key()));
  }

  @BeforeEach
  void setUp() throws URISyntaxException {
    openAIRequests = new LinkedBlockingDeque<>();
    app = Javalin.create();
    app.before(
        PATH,
        ctx ->
            openAIRequests.add(
                new OutboundRequest(ctx.body(), ctx.headerMap(), ctx.queryParamMap())));
    app.post(PATH, ctx -> ctx.result(MAPPER.writeValueAsString(SAMPLE_RESPONSE)));
    app.start(0);
    endpoint =
        URIBuilder.loopbackAddress().setScheme("http").appendPath(PATH).setPort(app.port()).build();
  }

  @Test
  void nonTextMessage() throws IOException, InterruptedException {
    String apiKey = UUID.randomUUID().toString();
    OpenAIConfig config = OpenAIConfig.builder(OpenAIModel.GPT4, apiKey).build();
    OpenAIPlugin<FBMessage> plugin = new OpenAIPlugin<FBMessage>(config).endpoint(endpoint);
    ThreadState<FBMessage> thread =
        THREAD
            .with(
                new FBMessage(
                    Instant.now(),
                    Identifier.random(),
                    THREAD.tail().senderId(),
                    THREAD.tail().recipientId(),
                    new Payload.Image(new byte[0], "image/jpeg"),
                    Role.USER))
            .with(
                new FBMessage(
                    Instant.now(),
                    Identifier.random(),
                    THREAD.tail().senderId(),
                    THREAD.tail().recipientId(),
                    new Payload.Document(new byte[0], "application/pdf"),
                    Role.USER));

    FBMessage message = plugin.handle(thread);
    assertThat(message.message()).isEqualTo(TEST_MESSAGE);
    assertThat(message.role()).isSameAs(Role.ASSISTANT);
    @Nullable OutboundRequest or = openAIRequests.poll(500, TimeUnit.MILLISECONDS);
    assertThat(MAPPER.readTree(or.body()).get("messages"))
        .hasSize(2); // system message + existing text message
  }

  @ParameterizedTest
  @EnumSource(OpenAIModel.class)
  void sampleValid(OpenAIModel model) throws IOException, InterruptedException {
    String apiKey = UUID.randomUUID().toString();
    OpenAIConfig config = OpenAIConfig.builder(model, apiKey).build();
    OpenAIPlugin<FBMessage> plugin = new OpenAIPlugin<FBMessage>(config).endpoint(endpoint);
    FBMessage message = plugin.handle(THREAD);
    assertThat(message.message()).isEqualTo(TEST_MESSAGE);
    assertThat(message.role()).isSameAs(Role.ASSISTANT);
    assertThatCode(() -> THREAD.with(message)).doesNotThrowAnyException();
    @Nullable OutboundRequest or = openAIRequests.poll(500, TimeUnit.MILLISECONDS);
    assertThat(or).isNotNull();
    assertThat(or.headerMap().get("Authorization")).isNotNull().isEqualTo("Bearer " + apiKey);
    assertThat(MAPPER.readTree(or.body()).get("model").textValue()).isEqualTo(model.toString());
  }

  @BeforeEach
  void setUpMinConfig() {
    minimalConfig = MAPPER.createObjectNode();
    OpenAIConfigTest.CONFIG_ITEMS.forEach(
        t -> {
          if (t.required()) {
            minimalConfig.set(t.key(), t.validValue());
          }
        });
  }

  @ParameterizedTest
  @MethodSource("modelOptions")
  void validConfigValues(OpenAIConfigTest.ConfigItem configItem)
      throws IOException, InterruptedException {
    minimalConfig.set(configItem.key(), configItem.validValue());
    OpenAIConfig config =
        ConfigurationUtils.jsonMapper().convertValue(minimalConfig, OpenAIConfig.class);
    OpenAIPlugin<FBMessage> plugin = new OpenAIPlugin<FBMessage>(config).endpoint(endpoint);
    FBMessage message = plugin.handle(THREAD);
    assertThat(message.message()).isEqualTo(TEST_MESSAGE);
    assertThat(message.role()).isSameAs(Role.ASSISTANT);
    assertThatCode(() -> THREAD.with(message)).doesNotThrowAnyException();
    @Nullable OutboundRequest or = openAIRequests.poll(500, TimeUnit.MILLISECONDS);
    assertThat(or).isNotNull();
    assertThat(or.headerMap().get("Authorization"))
        .isNotNull()
        .isEqualTo("Bearer " + config.apiKey());
    JsonNode body = ConfigurationUtils.jsonMapper().readTree(or.body());
    assertThat(body.get("model").textValue()).isEqualTo(config.model().toString());
    if (configItem.key().equals("system_message")) {
      assertThat(body.get("messages"))
          .satisfiesOnlyOnce(
              m -> {
                assertThat(m.get("role").textValue()).isEqualTo("system");
                assertThat(m.get("content").textValue())
                    .isEqualTo(configItem.validValue().textValue());
              });
    } else {
      if (configItem.key().equals("max_output_tokens")) {
        assertThat(body.get("max_tokens")).isEqualTo(minimalConfig.get(configItem.key()));
      } else {
        assertThat(body.get(configItem.key())).isEqualTo(minimalConfig.get(configItem.key()));
      }
    }
  }

  @Test
  void contextTooBig() throws IOException {
    OpenAIConfig config =
        OpenAIConfig.builder(OpenAIModel.GPT35TURBO, "lkjasdlkjasdf").maxInputTokens(100).build();
    OpenAIPlugin<FBMessage> plugin = new OpenAIPlugin<FBMessage>(config).endpoint(endpoint);
    ThreadState<FBMessage> thread =
        THREAD.with(
            THREAD.newMessageFromUser(
                Instant.now(),
                Stream.generate(() -> "0123456789").limit(100).collect(Collectors.joining()),
                Identifier.random()));
    FBMessage response = plugin.handle(thread);
    assertThat(response.message()).isEqualTo("I'm sorry but that request was too long for me.");
    assertThat(openAIRequests).hasSize(0);
  }

  @Test
  void orderedCorrectly() throws IOException, InterruptedException {
    OpenAIConfig config =
        OpenAIConfig.builder(OpenAIModel.GPT35TURBO, "lkjasdlkjasdf").maxInputTokens(100).build();
    OpenAIPlugin<FBMessage> plugin = new OpenAIPlugin<FBMessage>(config).endpoint(endpoint);
    ThreadState<FBMessage> thread =
        ThreadState.of(
            MessageFactory.instance(FBMessage.class)
                .newMessage(
                    Instant.now(),
                    new Payload.Text("1"),
                    Identifier.random(),
                    Identifier.random(),
                    Identifier.random(),
                    Role.USER));
    thread = thread.with(thread.newMessageFromUser(Instant.now(), "2", Identifier.from(2)));
    thread = thread.with(thread.newMessageFromUser(Instant.now(), "3", Identifier.from(3)));
    thread = thread.with(thread.newMessageFromUser(Instant.now(), "4", Identifier.from(4)));
    plugin.handle(thread);
    @Nullable OutboundRequest or = openAIRequests.poll(500, TimeUnit.MILLISECONDS);
    assertThat(or).isNotNull();
    JsonNode body = MAPPER.readTree(or.body());

    String systemMessage = body.get("messages").get(0).get("content").textValue();
    assertThat(systemMessage).isEqualTo(config.systemMessage());

    for (int i = 0; i < thread.messages().size(); i++) {
      FBMessage threadMessage = thread.messages().get(i);
      JsonNode sentMessage = body.get("messages").get(i + 1); // system message is not in the stack
      assertSoftly(
          s ->
              s.assertThat(threadMessage.message())
                  .isEqualTo(sentMessage.get("content").textValue()));
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
        URIBuilder.loopbackAddress()
            .appendPath(metaPath)
            .setScheme("http")
            .setPort(app.port())
            .build();
    app.post(
        metaPath,
        ctx ->
            metaRequests.put(
                new OutboundRequest(ctx.body(), ctx.headerMap(), ctx.queryParamMap())));
    FBMessageHandler handler =
        new FBMessageHandler(verifyToken, accessToken, appSecret)
            .baseURLFactory(ignored -> messageReceiver);

    String apiKey = "api key";
    OpenAIConfig config = OpenAIConfig.builder(OpenAIModel.GPT4, apiKey).build();
    OpenAIPlugin<FBMessage> plugin = new OpenAIPlugin<FBMessage>(config).endpoint(endpoint);

    String webhookPath = "/webhook";
    Service<FBMessage> service = new Service<>(store, handler, plugin, webhookPath);
    ServicesRunner runner = ServicesRunner.newInstance().service(service).port(0);
    runner.start();

    // TODO: create test harness
    Request request =
        FBMessageHandlerTest.createMessageRequest(FBMessageHandlerTest.SAMPLE_MESSAGE, runner);
    HttpResponse response = request.execute().returnResponse();
    assertThat(response.getCode()).isEqualTo(200);
    @Nullable OutboundRequest or = openAIRequests.poll(500, TimeUnit.MILLISECONDS);
    assertThat(or).isNotNull();
    assertThat(or.headerMap().get("Authorization"))
        .isNotNull()
        .isEqualTo("Bearer " + config.apiKey());
    JsonNode body = ConfigurationUtils.jsonMapper().readTree(or.body());
    assertThat(body.get("model").textValue()).isEqualTo(config.model().toString());

    or = metaRequests.poll(500, TimeUnit.MILLISECONDS);
    // plugin output got back to meta
    assertThat(or).isNotNull().satisfies(r -> assertThat(r.body()).contains(TEST_MESSAGE));
  }

  private record OutboundRequest(
      String body, Map<String, String> headerMap, Map<String, List<String>> queryParamMap) {}
}
