/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.util.Collection;
import java.util.List;

public class ConfigurationUtils {

  private static final Collection<DeserializationFeatureConfig> DESERIALIZATION_FEATURES =
      List.of(
          new DeserializationFeatureConfig(
              DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true),
          new DeserializationFeatureConfig(DeserializationFeature.WRAP_EXCEPTIONS, true),
          new DeserializationFeatureConfig(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true),
          new DeserializationFeatureConfig(DeserializationFeature.USE_LONG_FOR_INTS, true),
          new DeserializationFeatureConfig(
              DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, false),
          new DeserializationFeatureConfig(
              DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false));

  private static final JsonMapper JSON_MAPPER;
  private static final TomlMapper TOML_MAPPER;

  static {
    JsonMapper.Builder builder = JsonMapper.builder();
    DESERIALIZATION_FEATURES.forEach(f -> builder.configure(f.feature(), f.state()));
    JSON_MAPPER = builder.build();
  }

  static {
    TomlMapper.Builder builder = TomlMapper.builder();
    DESERIALIZATION_FEATURES.forEach(f -> builder.configure(f.feature(), f.state()));
    TOML_MAPPER = builder.build();
  }

  public static JsonMapper jsonMapper() {
    return JSON_MAPPER;
  }

  public static TomlMapper tomlMapper() {
    return TOML_MAPPER;
  }

  private record DeserializationFeatureConfig(DeserializationFeature feature, boolean state) {}
}
