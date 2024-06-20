/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

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
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.Payload;
import com.meta.cp4m.message.ThreadState;
import com.meta.cp4m.message.WAMessage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
  void sanity() throws URISyntaxException, IOException, InterruptedException {
    GenericPlugin.GenericPluginThreadUpdateResponse res =
        new GenericPlugin.GenericPluginThreadUpdateResponse("text", "hello from the plugin!");
    JsonNode programmedResponse = MAPPER.valueToTree(res);
    webServer.response(programmedResponse);
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
    WAMessage response = plugin.handle(ts);
    ReceivedRequest received = webServer.take(500);
    assertThat(received)
        .satisfies(
            r -> assertThat(r.contentType()).isEqualTo(ContentType.APPLICATION_JSON.toString()))
        .satisfies(r -> assertThat(r.path()).isEqualTo(path));
    ObjectNode body = (ObjectNode) MAPPER.readTree(received.body());
    assertThat(body.get("user").get("id").textValue()).isEqualTo(ts.userId().toString());
    assertThat(body.get("timestamp")).isNotNull();
    assertThat(body.get("messages"))
        .hasSize(1)
        .allSatisfy(m -> assertThat(m.get("type").textValue()).isEqualTo("text"))
        .allSatisfy(m -> assertThat(m.get("value").textValue()).isEqualTo("hello world!"))
        .allSatisfy(
            m -> assertThat(m.get("timestamp").textValue()).isEqualTo(messageTime.toString()));
    assertThat(response)
        .satisfies(r -> assertThat(r.recipientId()).isEqualTo(ts.tail().senderId()))
        .satisfies(r -> assertThat(r.senderId()).isEqualTo(ts.tail().recipientId()))
        .satisfies(r -> assertThat(r.threadId()).isEqualTo(ts.tail().threadId()))
        .satisfies(r -> assertThat(r.payload().value()).isEqualTo(res.value()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"{}", "{\ninvalidjson:\"", "{\"type\": \"invalidtype\", \"value\": \"\"}"})
  void invalidPayloadFormat(String programmedResponse)
      throws URISyntaxException, InterruptedException {
    webServer.response(programmedResponse);
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
