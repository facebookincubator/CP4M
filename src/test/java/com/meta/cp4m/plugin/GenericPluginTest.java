/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import static com.meta.cp4m.message.MessageFactory.FACTORY_MAP;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.meta.cp4m.DummyWebServer;
import com.meta.cp4m.DummyWebServer.ReceivedRequest;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.configuration.ConfigurationUtils;
import com.meta.cp4m.configuration.RootConfiguration;
import com.meta.cp4m.message.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.stream.Stream;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class GenericPluginTest {
  private static final JsonMapper MAPPER = new JsonMapper();
  private static final String TOML =
      """
port = 8081

[[plugins]]
name = "test_plugin"
type = "generic"
url = "http://example.com"

[[handlers]]
type = "messenger"
name = "messenger_test"
verify_token = "imgibberish"
app_secret = "imnotasecret"
page_access_token = "imnotasecreteither"

[[services]]
webhook_path = "/messenger"
plugin = "test_plugin"
handler = "messenger_test"
""";

  private static final String TOML_W_OAUTH =
      """
port = 8081

[[plugins]]
name = "test_plugin"
type = "generic"
url = "http://example.com"

[plugins.authentication]
type = "oauth2"
tenant_url = "http://example.com/oauth"
client_id = "test_client_id"
client_secret = "test_client_secret"
audience = "audience"

[[handlers]]
type = "messenger"
name = "messenger_test"
verify_token = "imgibberish"
app_secret = "imnotasecret"
page_access_token = "imnotasecreteither"

[[services]]
webhook_path = "/messenger"
plugin = "test_plugin"
handler = "messenger_test"
""";

  private final DummyWebServer webServer = DummyWebServer.create();

  @Test
  void config() throws JsonProcessingException {
    TomlMapper mapper = ConfigurationUtils.tomlMapper();
    RootConfiguration config =
        ConfigurationUtils.tomlMapper()
            .convertValue(mapper.readTree(TOML), RootConfiguration.class);

    assertThat(config.toServicesRunner().services())
        .hasSize(1)
        .allSatisfy(p -> assertThat(p.plugin()).isOfAnyClassIn(GenericPlugin.class));
  }

  @Test
  void configWAuth() throws JsonProcessingException {
    TomlMapper mapper = ConfigurationUtils.tomlMapper();
    JsonNode tree = mapper.readTree(TOML_W_OAUTH);
    RootConfiguration config =
        ConfigurationUtils.tomlMapper().convertValue(tree, RootConfiguration.class);

    assertThat(config.toServicesRunner().services())
        .hasSize(1)
        .allSatisfy(p -> assertThat(p.plugin()).isOfAnyClassIn(GenericPlugin.class));
  }

  static Stream<ThreadState<?>> statesWithUserData() {
    Identifier businessId = Identifier.random();
    Identifier userId = Identifier.random();
    Instant messageTime = Instant.now();
    return FACTORY_MAP.values().stream()
        .map(
            f ->
                ThreadState.of(
                    f.newMessage(
                        messageTime,
                        new Payload.Text("hello world!'\"!@#$%^&*()’"),
                        userId,
                        businessId,
                        Identifier.random(),
                        Message.Role.USER)))
        .flatMap(
            ts ->
                Stream.of(
                    ts,
                    ts.withUserData(ts.userData().withName("test name")),
                    ts.withUserData(ts.userData().withPhoneNumber("1234567890")),
                    ts.withUserData(ts.userData().withPhoneNumber("1234567899")),
                    ts.withUserData(
                        ts.userData().withName("test name").withPhoneNumber("1234567899"))));
  }

  @ParameterizedTest
  @MethodSource("statesWithUserData")
  <T extends Message> void sanity(ThreadState<T> ts)
      throws URISyntaxException, IOException, InterruptedException {
    GenericPlugin.GenericPluginThreadUpdateResponse res =
        new GenericPlugin.GenericPluginThreadUpdateResponse("text", "hello from the plugin!");
    JsonNode programmedResponse = MAPPER.valueToTree(res);
    webServer.response(ignored -> true, programmedResponse);
    final String path = "/generic";
    final URI url =
        URIBuilder.loopbackAddress()
            .appendPath(path)
            .setScheme("http")
            .setPort(webServer.port())
            .build();
    GenericPlugin<T> plugin = new GenericPlugin<>(url);
    T response = plugin.handle(ts);
    ReceivedRequest received = webServer.take(500);
    assertThat(received)
        .satisfies(
            r -> assertThat(r.contentType()).isEqualTo(ContentType.APPLICATION_JSON.toString()))
        .satisfies(r -> assertThat(r.path()).isEqualTo(path));
    ObjectNode body = (ObjectNode) MAPPER.readTree(received.body());
    switch (ts.tail()) {
      case WAMessage ignored ->
          assertThat(body.get("source_client").textValue()).isEqualTo("whatsapp");
      case FBMessage ignored ->
          assertThat(body.get("source_client").textValue()).isEqualTo("messenger");
      default -> fail("all cases should be covered");
    }
    ObjectNode user = (ObjectNode) body.get("user");
    assertThat(user.get("id").textValue()).isEqualTo(ts.userId().toString());
    if (ts.userData().phoneNumber().isPresent()) {
      assertThat(user.get("phone_number").textValue()).isEqualTo(ts.userData().phoneNumber().get());
    } else {
      assertThat(user.get("phone_number")).isNull();
    }
    if (ts.userData().name().isPresent()) {
      assertThat(user.get("name").textValue()).isEqualTo(ts.userData().name().get());
    } else {
      assertThat(user.get("name")).isNull();
    }
    assertThat(body.get("timestamp").textValue()).isNotNull();
    assertThat(body.get("messages"))
        .hasSize(1)
        .allSatisfy(m -> assertThat(m.get("type").textValue()).isEqualTo("text"))
        .allSatisfy(m -> assertThat(m.get("value").textValue()).isEqualTo(ts.tail().message()))
        .allSatisfy(
            m ->
                assertThat(m.get("timestamp").textValue())
                    .isEqualTo(ts.tail().timestamp().toString()))
        .allSatisfy(
            m ->
                assertThat(m.get("timestamp").textValue())
                    .isEqualTo(ts.tail().timestamp().toString()));
    assertThat(response)
        .satisfies(r -> assertThat(r.recipientId()).isEqualTo(ts.tail().senderId()))
        .satisfies(r -> assertThat(r.senderId()).isEqualTo(ts.tail().recipientId()))
        .satisfies(r -> assertThat(r.threadId()).isEqualTo(ts.tail().threadId()))
        .satisfies(r -> assertThat(r.payload().value()).isEqualTo(res.value()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"{}", "{\ninvalidjson:\"", "{\"type\": \"invalidtype\", \"value\": \"\"}", ""})
  void invalidPayloadFormat(String programmedResponse)
      throws URISyntaxException, InterruptedException {
    webServer.response(ignored -> true, programmedResponse);
    final String path = "/generic";
    final URI url =
        URIBuilder.loopbackAddress()
            .appendPath(path)
            .setScheme("http")
            .setPort(webServer.port())
            .build();
    GenericPlugin<WAMessage> plugin = new GenericPlugin<>(url);
    Identifier businessId = Identifier.random();
    Identifier userId = Identifier.random();
    Instant messageTime = Instant.now();
    ThreadState<WAMessage> ts =
        ThreadState.of(
            new WAMessage(
                messageTime,
                Identifier.random(),
                userId,
                businessId,
                new Payload.Text("hello world!"),
                Message.Role.USER));
    assertThatThrownBy(() -> plugin.handle(ts)).isInstanceOf(IOException.class);
    @Nullable ReceivedRequest received = webServer.poll(10);
    assertThat(received).isNotNull();
  }
}
