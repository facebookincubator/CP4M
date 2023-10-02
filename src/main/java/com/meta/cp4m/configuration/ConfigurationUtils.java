/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.google.common.io.Files;
import java.io.IOException;
import java.nio.file.Path;
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

  public static RootConfiguration loadConfigurationFile(Path file) throws IOException {
    String extension = Files.getFileExtension(file.toString());
    if (extension.equals("json")) {
      return jsonMapper().readValue(file.toFile(), RootConfiguration.class);
    } else if (extension.equals("toml")) {
      return tomlMapper().readValue(file.toFile(), RootConfiguration.class);
    }
    throw new IOException(
        "Unknown file extension '" + extension + "', extension must be either json or toml.");
  }

  private record DeserializationFeatureConfig(DeserializationFeature feature, boolean state) {}
}
