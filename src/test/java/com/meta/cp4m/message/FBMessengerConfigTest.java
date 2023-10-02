/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meta.cp4m.configuration.ConfigurationUtils;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class FBMessengerConfigTest {

  private static final Collection<ConfigParamTestSpec<FBMessengerConfig>> PARAMS =
      List.of(
          ConfigParamTestSpec.of(FBMessengerConfig.class, "type")
              .validValues("messenger")
              .required(true),
          ConfigParamTestSpec.of(FBMessengerConfig.class, "name")
              .required(true)
              .validValues("hi")
              .invalidValues("", "  ")
              .getter(FBMessengerConfig::name),
          ConfigParamTestSpec.of(FBMessengerConfig.class, "verify_token")
              .required(true)
              .validValues("123")
              .invalidValues("", "  ")
              .getter(FBMessengerConfig::verifyToken),
          ConfigParamTestSpec.of(FBMessengerConfig.class, "app_secret")
              .required(true)
              .validValues("123")
              .invalidValues("", " ")
              .getter(FBMessengerConfig::appSecret),
          ConfigParamTestSpec.of(FBMessengerConfig.class, "page_access_token")
              .required(true)
              .validValues("123")
              .invalidValues("", " ")
              .getter(FBMessengerConfig::pageAccessToken));

  static Stream<Named<ConfigParamTestSpec<FBMessengerConfig>>> required() {
    return PARAMS.stream().filter(ConfigParamTestSpec::required).map(p -> Named.of(p.name(), p));
  }

  static Stream<Named<ConfigParamTestSpec<FBMessengerConfig>>> params() {
    return PARAMS.stream().map(p -> Named.of(p.name(), p));
  }

  ObjectNode minimalConfig() {
    ObjectNode node = ConfigurationUtils.jsonMapper().createObjectNode();
    PARAMS.stream()
        .filter(ConfigParamTestSpec::required)
        .forEach(p -> node.set(p.name(), p.validValues().stream().findAny().orElseThrow()));
    return node;
  }

  @Test
  void minimalValid() {
    ConfigurationUtils.jsonMapper().convertValue(minimalConfig(), FBMessengerConfig.class);
  }

  @ParameterizedTest
  @MethodSource("params")
  void allValid(ConfigParamTestSpec<FBMessengerConfig> param) {
    ObjectNode config = minimalConfig();
    for (JsonNode validValue : param.validValues()) {
      config.set(param.name(), validValue);
      FBMessengerConfig configObj =
          ConfigurationUtils.jsonMapper().convertValue(config, FBMessengerConfig.class);
      if (!param.name().equals("type")) {
        assertThat(validValue).isEqualTo(param.get(configObj));
      }
    }
  }

  @ParameterizedTest
  @MethodSource("params")
  void allInvalid(ConfigParamTestSpec<FBMessengerConfig> param) {
    JsonMapper mapper = ConfigurationUtils.jsonMapper();
    ObjectNode config = minimalConfig();
    for (JsonNode invalidValue : param.invalidValues()) {
      config.set(param.name(), invalidValue);
      assertThatThrownBy(() -> mapper.convertValue(config, FBMessengerConfig.class))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @ParameterizedTest
  @MethodSource("required")
  void requiredTest(ConfigParamTestSpec<FBMessengerConfig> param) {
    ObjectNode config = minimalConfig();
    config.remove(param.name());
    assertThatThrownBy(
            () -> ConfigurationUtils.jsonMapper().convertValue(config, FBMessengerConfig.class))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
