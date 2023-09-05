/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RootConfigurationTestUtils {
  private static final String TOML =
      """
[[plugins]]
name = "openai_test"
type = "openai"
model = "gpt-3.5-turbo"
api_key = "abc123"
""";

  @Test
  void test(@TempDir Path dir) throws IOException {
    Path configFile = dir.resolve("config.toml");
    Files.writeString(configFile, TOML);
    TomlMapper mapper = new TomlMapper();
    JsonNode node = mapper.readTree(configFile.toFile());
    RootConfiguration config =
        ConfigurationUtils.jsonMapper().convertValue(node, RootConfiguration.class);
    config.plugins();
  }
}
