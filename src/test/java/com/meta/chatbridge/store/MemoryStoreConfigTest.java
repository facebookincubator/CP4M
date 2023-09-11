/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meta.chatbridge.configuration.ConfigurationUtils;
import com.meta.chatbridge.message.ConfigParamTestSpec;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MemoryStoreConfigTest {

  private static final Collection<ConfigParamTestSpec<MemoryStoreConfig>> PARAMS =
      List.of(
          ConfigParamTestSpec.of(MemoryStoreConfig.class, "type")
              .validValues("memory")
              .invalidValues("", "junk")
              .required(true),
          ConfigParamTestSpec.of(MemoryStoreConfig.class, "name")
              .validValues("anything")
              .invalidValues("", "  ")
              .required(true)
              .getter(MemoryStoreConfig::name),
          ConfigParamTestSpec.of(MemoryStoreConfig.class, "storage_duration_hours")
              .validValues(1, 200)
              .invalidValues(0, -1)
              .required(true)
              .getter(MemoryStoreConfig::storageDurationHours),
          ConfigParamTestSpec.of(MemoryStoreConfig.class, "storage_capacity_mbs")
              .validValues(1, 100)
              .invalidValues(0, -1)
              .required(true)
              .getter(MemoryStoreConfig::storageCapacityMb));

  static Stream<Named<ConfigParamTestSpec<MemoryStoreConfig>>> required() {
    return PARAMS.stream().filter(ConfigParamTestSpec::required).map(p -> Named.of(p.name(), p));
  }

  static Stream<Named<ConfigParamTestSpec<MemoryStoreConfig>>> params() {
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
    ConfigurationUtils.jsonMapper().convertValue(minimalConfig(), MemoryStoreConfig.class);
  }

  @ParameterizedTest
  @MethodSource("params")
  void allValid(ConfigParamTestSpec<MemoryStoreConfig> param) {
    ObjectNode config = minimalConfig();
    for (JsonNode validValue : param.validValues()) {
      config.set(param.name(), validValue);
      MemoryStoreConfig configObj =
          ConfigurationUtils.jsonMapper().convertValue(config, MemoryStoreConfig.class);
      if (!param.name().equals("type")) {
        assertThat(validValue).isEqualTo(param.get(configObj));
      }
    }
  }

  @ParameterizedTest
  @MethodSource("params")
  void allInvalid(ConfigParamTestSpec<MemoryStoreConfig> param) {
    JsonMapper mapper = ConfigurationUtils.jsonMapper();
    ObjectNode config = minimalConfig();
    for (JsonNode invalidValue : param.invalidValues()) {
      config.set(param.name(), invalidValue);
      assertThatThrownBy(() -> mapper.convertValue(config, MemoryStoreConfig.class))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @ParameterizedTest
  @MethodSource("required")
  void requiredTest(ConfigParamTestSpec<MemoryStoreConfig> param) {
    ObjectNode config = minimalConfig();
    config.remove(param.name());
    assertThatThrownBy(
            () -> ConfigurationUtils.jsonMapper().convertValue(config, MemoryStoreConfig.class))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
