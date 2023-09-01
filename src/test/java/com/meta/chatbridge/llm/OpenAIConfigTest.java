/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.collect.ImmutableList;
import com.meta.chatbridge.Configuration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OpenAIConfigTest {

  private static final ObjectMapper MAPPER = Configuration.MAPPER;
  static final Collection<ConfigItem> CONFIG_ITEMS =
      ImmutableList.of(
          new ConfigItem("model", true, TextNode.valueOf("gpt-4"), List.of(TextNode.valueOf("n"))),
          new ConfigItem(
              "api_key",
              true,
              TextNode.valueOf("notempty"),
              List.of(TextNode.valueOf(""), TextNode.valueOf("   "))),
          new ConfigItem(
              "temperature",
              false,
              DoubleNode.valueOf(1),
              List.of(DoubleNode.valueOf(-0.1), DoubleNode.valueOf(2.1))),
          new ConfigItem(
              "top_p",
              false,
              DoubleNode.valueOf(0.5),
              List.of(DoubleNode.valueOf(0), DoubleNode.valueOf(1.1))),
          new ConfigItem(
              "max_tokens",
              false,
              LongNode.valueOf(100),
              List.of(
                  LongNode.valueOf(0),
                  LongNode.valueOf(OpenAIModel.GPT4.properties().tokenLimit() + 1))),
          new ConfigItem(
              "presence_penalty",
              false,
              DoubleNode.valueOf(0),
              List.of(DoubleNode.valueOf(-2.1), DoubleNode.valueOf(2.1))),
          new ConfigItem(
              "frequency_penalty",
              false,
              DoubleNode.valueOf(0),
              List.of(DoubleNode.valueOf(-2.1), DoubleNode.valueOf(2.1))),
          new ConfigItem(
              "logit_bias",
              false,
              MAPPER.createObjectNode().put("1", 0.5),
              List.of(
                  MAPPER.createObjectNode().put("1", -101),
                  MAPPER.createObjectNode().put("1", 101))),
          new ConfigItem(
              "system_message",
              false,
              TextNode.valueOf("you're a helpful assistant"),
              List.of(TextNode.valueOf(""), TextNode.valueOf("  "))));
  private ObjectNode minimalConfig;

  static Stream<ConfigItem> configItems() {
    return CONFIG_ITEMS.stream();
  }

  static Stream<Arguments> invalidValues() {
    return configItems()
        .flatMap(c -> c.invalidValues().stream().map(t -> Arguments.of(c.key(), t)));
  }

  static Stream<String> requiredKeys() {
    return configItems().filter(ConfigItem::required).map(ConfigItem::key);
  }

  @BeforeEach
  void setUp() {
    minimalConfig = MAPPER.createObjectNode();
    CONFIG_ITEMS.forEach(
        t -> {
          if (t.required()) {
            minimalConfig.set(t.key(), t.validValue());
          }
        });
  }

  @Test
  void maximalValidConfig() throws JsonProcessingException {
    ObjectNode body = MAPPER.createObjectNode();
    CONFIG_ITEMS.forEach(t -> body.set(t.key(), t.validValue()));
    OpenAIConfig config = MAPPER.readValue(MAPPER.writeValueAsString(body), OpenAIConfig.class);
    assertThat(config.model()).isEqualTo(OpenAIModel.GPT4);
    assertThat(config.temperature().isPresent()).isTrue();
    assertThat(config.frequencyPenalty().isPresent()).isTrue();
    assertThat(config.topP().isPresent()).isTrue();
    assertThat(config.maxTokens().isPresent()).isTrue();
    assertThat(config.presencePenalty().isPresent()).isTrue();
    assertThat(config.frequencyPenalty().isPresent()).isTrue();
    assertThat(config.logitBias().isEmpty()).isFalse();
  }

  @Test
  void minimalValidConfig() throws JsonProcessingException {
    ObjectNode body = MAPPER.createObjectNode();
    CONFIG_ITEMS.forEach(
        t -> {
          if (t.required()) {
            body.set(t.key(), t.validValue());
          }
        });
    OpenAIConfig config = MAPPER.readValue(MAPPER.writeValueAsString(body), OpenAIConfig.class);
    assertThat(config.model()).isEqualTo(OpenAIModel.GPT4);
    assertThat(config.temperature().isEmpty()).isTrue();
    assertThat(config.frequencyPenalty().isEmpty()).isTrue();
    assertThat(config.topP().isEmpty()).isTrue();
    assertThat(config.maxTokens().isEmpty()).isTrue();
    assertThat(config.presencePenalty().isEmpty()).isTrue();
    assertThat(config.frequencyPenalty().isEmpty()).isTrue();
    assertThat(config.logitBias().isEmpty()).isTrue();
  }

  @ParameterizedTest
  @MethodSource("configItems")
  void nullValues(ConfigItem item) throws JsonProcessingException {
    minimalConfig.putNull(item.key());
    String bodyString = MAPPER.writeValueAsString(minimalConfig);
    assertThatThrownBy(() -> MAPPER.readValue(bodyString, OpenAIConfig.class))
        .isInstanceOf(Exception.class);
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  void invalidValues(String key, JsonNode value) throws JsonProcessingException {
    minimalConfig.set(key, value);
    String bodyString = MAPPER.writeValueAsString(minimalConfig);
    assertThatThrownBy(() -> MAPPER.readValue(bodyString, OpenAIConfig.class))
        .isInstanceOf(Exception.class);
  }

  @ParameterizedTest
  @MethodSource("requiredKeys")
  void requiredKeysMissing(String key) throws JsonProcessingException {
    minimalConfig.remove(key);
    String bodyString = MAPPER.writeValueAsString(minimalConfig);
    assertThatThrownBy(() -> MAPPER.readValue(bodyString, OpenAIConfig.class))
        .isInstanceOf(Exception.class);
  }

  record ConfigItem(
      String key, boolean required, JsonNode validValue, List<JsonNode> invalidValues) {}
}
