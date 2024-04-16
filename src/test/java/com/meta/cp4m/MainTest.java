/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.meta.cp4m.configuration.ConfigurationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ExtendWith(SystemStubsExtension.class)
class MainTest {
  private static final TomlMapper TOML_MAPPER = ConfigurationUtils.tomlMapper();
  private static final JsonMapper JSON_MAPPER = ConfigurationUtils.jsonMapper();
  private static final String TOML =
      """
port = 0

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

  private static final String JSON;

  static {
    try {
      JSON = JSON_MAPPER.writeValueAsString(TOML_MAPPER.readTree(TOML));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @SystemStub private EnvironmentVariables environmentVariables;

  @Test
  void commandlineArgumentToml(@TempDir Path dir) throws IOException {
    Path configFile = dir.resolve("configuration.toml");
    Files.writeString(configFile, TOML);
    Main.main(new String[] {"--config", configFile.toString()});
  }

  @Test
  void commandlineArgumentJson(@TempDir Path dir) throws IOException {
    Path configFile = dir.resolve("configuration.json");
    Files.writeString(configFile, JSON);
    Main.main(new String[] {"--config", configFile.toString()});
  }

  @Test
  void systemPropToml(@TempDir Path dir) throws IOException {
    Path configFile = dir.resolve("configuration.toml");
    Files.writeString(configFile, TOML);
    System.setProperty("cp4m_configuration_file", configFile.toString());
    Main.main(new String[] {});
  }

  @Test
  void systemPropJson(@TempDir Path dir) throws IOException {
    Path configFile = dir.resolve("configuration.json");
    Files.writeString(configFile, JSON);
    System.setProperty("cp4m_configuration_file", configFile.toString());
    Main.main(new String[] {});
  }

  @Test
  void envVarToml(@TempDir Path dir) throws IOException {
    Path configFile = dir.resolve("configuration.toml");
    environmentVariables.set("CP4M_CONFIGURATION_FILE", configFile.toString());
    Files.writeString(configFile, TOML);
    Main.main(new String[] {});
  }

  @Test
  void envVarJson(@TempDir Path dir) throws IOException {
    Path configFile = dir.resolve("configuration.json");
    environmentVariables.set("CP4M_CONFIGURATION_FILE", configFile.toString());
    Files.writeString(configFile, JSON);
    Main.main(new String[] {});
  }
}
