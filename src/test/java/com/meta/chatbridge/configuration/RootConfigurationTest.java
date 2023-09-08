/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.meta.chatbridge.ServiceConfiguration;
import com.meta.chatbridge.llm.OpenAIConfig;
import com.meta.chatbridge.llm.OpenAIModel;
import com.meta.chatbridge.message.FBMessengerConfig;
import com.meta.chatbridge.store.MemoryStoreConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RootConfigurationTest {
  private static final String TOML =
      """
port = 8081

[[plugins]]
name = "openai_test"
type = "openai"
model = "gpt-3.5-turbo"
api_key = "abc123"

[[stores]]
name = "memory_test"
type = "memory"
storage_duration_hours = 1
storage_capacity_mbs = 1

[[handlers]]
type = "messenger"
name = "messenger_test"
verify_token = "imgibberish"
app_secret = "imnotasecret"
page_access_token = "imnotasecreteither"

[[services]]
webhook_path = "/messenger"
plugin = "openai_test"
store = "memory_test"
handler = "messenger_test"
""";

  @Test
  void valid(@TempDir Path dir) throws IOException {
    Path configFile = dir.resolve("config.toml");
    Files.writeString(configFile, TOML);
    RootConfiguration config =
        ConfigurationUtils.tomlMapper().readValue(configFile.toFile(), RootConfiguration.class);
    assertThat(config.plugins())
        .hasSize(1)
        .allSatisfy(p -> assertThat(p).isInstanceOf(OpenAIConfig.class));
    OpenAIConfig pluginConfig = (OpenAIConfig) config.plugins().stream().findAny().orElseThrow();
    assertThat(pluginConfig.name()).isEqualTo("openai_test");
    assertThat(pluginConfig.model()).isEqualTo(OpenAIModel.GPT35TURBO);
    assertThat(pluginConfig.apiKey()).isEqualTo("abc123");

    assertThat(config.stores())
        .hasSize(1)
        .allSatisfy(s -> assertThat(s).isInstanceOf(MemoryStoreConfig.class));
    MemoryStoreConfig store = (MemoryStoreConfig) config.stores().stream().findAny().orElseThrow();
    assertThat(store.name()).isEqualTo("memory_test");
    assertThat(store.storageCapacityMb()).isEqualTo(1);
    assertThat(store.storageCapacityMb()).isEqualTo(1);

    assertThat(config.handlers())
        .hasSize(1)
        .allSatisfy(s -> assertThat(s).isInstanceOf(FBMessengerConfig.class));
    FBMessengerConfig handler =
        (FBMessengerConfig) config.handlers().stream().findAny().orElseThrow();
    assertThat(handler.name()).isEqualTo("messenger_test");
    assertThat(handler.verifyToken()).isEqualTo("imgibberish");
    assertThat(handler.appSecret()).isEqualTo("imnotasecret");
    assertThat(handler.pageAccessToken()).isEqualTo("imnotasecreteither");

    assertThat(config.services())
        .hasSize(1)
        .allSatisfy(s -> assertThat(s).isInstanceOf(ServiceConfiguration.class));
    ServiceConfiguration serviceConfiguration = config.services().stream().findAny().orElseThrow();
    assertThat(serviceConfiguration.webhookPath()).isEqualTo("/messenger");
    assertThat(serviceConfiguration.store()).isEqualTo("memory_test");
    assertThat(serviceConfiguration.plugin()).isEqualTo("openai_test");
    assertThat(serviceConfiguration.handler()).isEqualTo("messenger_test");

    assertThat(config.port()).isEqualTo(8081);
    config.toServicesRunner();
  }

  @Test
  void portDefaults8080() throws JsonProcessingException {
    TomlMapper mapper = ConfigurationUtils.tomlMapper();
    ObjectNode node = (ObjectNode) mapper.readTree(TOML);
    node.remove("port");
    RootConfiguration config = mapper.convertValue(node, RootConfiguration.class);
    assertThat(config.port()).isEqualTo(8080);
  }

  @ParameterizedTest
  @ValueSource(strings = {"handlers", "plugins", "stores", "services"})
  void requiredNonEmpty(String param) throws JsonProcessingException {
    TomlMapper mapper = ConfigurationUtils.tomlMapper();
    ObjectNode node = (ObjectNode) mapper.readTree(TOML);
    node.remove(param);
    assertThatThrownBy(() -> mapper.convertValue(node, RootConfiguration.class))
        .withFailMessage(param + " is required to be present")
        .isInstanceOf(IllegalArgumentException.class);

    node.putArray(param);
    assertThatThrownBy(() -> mapper.convertValue(node, RootConfiguration.class))
        .withFailMessage(param + " is required to be non-empty")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"store", "plugin", "handler"})
  void serviceValuesMustMatch(String param) throws JsonProcessingException {
    TomlMapper mapper = ConfigurationUtils.tomlMapper();
    ObjectNode node = (ObjectNode) mapper.readTree(TOML);
    ObjectNode serviceConfig = (ObjectNode) node.get("services").get(0);
    serviceConfig.put(param, "junk value");
    assertThatThrownBy(() -> mapper.convertValue(node, RootConfiguration.class))
        .withFailMessage("the services value for " + param + " must be present in a " + param)
        .isInstanceOf(IllegalArgumentException.class);
  }
}
