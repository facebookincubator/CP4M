/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.configuration.ConfigurationUtils;
import com.meta.cp4m.configuration.RootConfiguration;
import com.meta.cp4m.message.FBMessage;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EchoPluginTest {

  private static final String TOML =
      """
port = 8081

[[plugins]]
name = "echo_test"
type = "echo"

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
plugin = "echo_test"
store = "memory_test"
handler = "messenger_test"
""";

  @Test
  void sanity() throws IOException {
    EchoPlugin<FBMessage> plugin = new EchoPlugin<>();
    FBMessage output =
        plugin.handle(
            ThreadState.of(
                new FBMessage(
                    Instant.now(),
                    Identifier.random(),
                    Identifier.random(),
                    Identifier.random(),
                    "test",
                    Message.Role.USER)));
    assertThat(output.message()).isEqualTo("test");
  }

  @Test
  void configLoads() throws JsonProcessingException {
    TomlMapper mapper = ConfigurationUtils.tomlMapper();
    RootConfiguration config =
        ConfigurationUtils.tomlMapper()
            .convertValue(mapper.readTree(TOML), RootConfiguration.class);

    assertThat(config.toServicesRunner().services())
        .hasSize(1)
        .allSatisfy(p -> assertThat(p.plugin()).isOfAnyClassIn(EchoPlugin.class));
  }
}
