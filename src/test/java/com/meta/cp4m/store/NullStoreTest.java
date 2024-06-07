/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.configuration.ConfigurationUtils;
import com.meta.cp4m.configuration.RootConfiguration;
import com.meta.cp4m.message.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NullStoreTest {
  private static final String TOML =
      """
  port = 8081

  [[plugins]]
  name = "mirror_test"
  type = "mirror"

  [[stores]]
  name = "null_test"
  type = "null"

  [[handlers]]
  type = "messenger"
  name = "messenger_test"
  verify_token = "imgibberish"
  app_secret = "imnotasecret"
  page_access_token = "imnotasecreteither"

  [[services]]
  webhook_path = "/messenger"
  plugin = "mirror_test"
  store = "null_test"
  handler = "messenger_test"
  """;

  @Test
  void test() {
    Identifier senderId = Identifier.random();
    Identifier recipientId = Identifier.random();

    MessageFactory<FBMessage> messageFactory = MessageFactory.instance(FBMessage.class);
    NullStore<FBMessage> nullStore = new NullStore<>();

    assertThat(nullStore.size()).isEqualTo(0);
    FBMessage message =
        messageFactory.newMessage(
            Instant.now(), "", senderId, recipientId, Identifier.random(), Message.Role.ASSISTANT);
    ThreadState<FBMessage> thread = nullStore.add(message);
    assertThat(nullStore.size()).isEqualTo(0);
    assertThat(thread.messages()).hasSize(1).contains(message);

    FBMessage message2 =
        messageFactory.newMessage(
            Instant.now(), "", recipientId, senderId, Identifier.random(), Message.Role.USER);
    thread = nullStore.add(message2);
    assertThat(nullStore.size()).isEqualTo(0);
    assertThat(thread.messages()).hasSize(1);

    FBMessage message3 =
        messageFactory.newMessage(
            Instant.now(),
            "",
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
    thread = nullStore.add(message3);
    assertThat(nullStore.size()).isEqualTo(0);
    assertThat(thread.messages()).hasSize(1).contains(message3);
  }

  @Test
  void configLoads() throws JsonProcessingException {
    TomlMapper mapper = ConfigurationUtils.tomlMapper();
    RootConfiguration config =
        ConfigurationUtils.tomlMapper()
            .convertValue(mapper.readTree(TOML), RootConfiguration.class);

    assertThat(config.toServicesRunner().services())
        .hasSize(1)
        .allSatisfy(p -> assertThat(p.store()).isOfAnyClassIn(NullStore.class));
  }

  @Test
  void configLoadsWithoutDefinedStore() throws JsonProcessingException {
    TomlMapper mapper = ConfigurationUtils.tomlMapper();
    ObjectNode node = (ObjectNode) mapper.readTree(TOML);
    node.remove("stores");
    ((ObjectNode) node.get("services").get(0)).remove("store");
    RootConfiguration config =
        ConfigurationUtils.tomlMapper()
            .convertValue(mapper.readTree(TOML), RootConfiguration.class);

    assertThat(config.toServicesRunner().services())
        .hasSize(1)
        .allSatisfy(p -> assertThat(p.store()).isOfAnyClassIn(NullStore.class));
  }
}
