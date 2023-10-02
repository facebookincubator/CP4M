/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.meta.cp4m.configuration.ConfigurationUtils;
import java.util.*;
import java.util.function.Function;
import org.checkerframework.common.reflection.qual.NewInstance;

public class ConfigParamTestSpec<T> {

  private final Class<T> configType;
  private final String name;
  private final boolean required;
  private final boolean typeParam;
  private final Collection<JsonNode> validValues;

  private final Collection<JsonNode> invalidValues;

  private final Function<T, ?> getter;

  private ConfigParamTestSpec(
      Class<T> configType,
      String name,
      boolean required,
      boolean typeParam,
      Collection<JsonNode> validValues,
      Collection<JsonNode> invalidValues,
      Function<T, ?> getter) {
    this.configType = configType;
    this.name = name;
    this.required = required;
    this.typeParam = typeParam;
    this.validValues = validValues;
    this.invalidValues = invalidValues;
    this.getter = getter;
  }

  public static <T> ConfigParamTestSpec<T> of(Class<T> configType, String name) {
    return new ConfigParamTestSpec<>(
        configType,
        name,
        false,
        false,
        Collections.emptyList(),
        Collections.emptyList(),
        n -> NullNode.getInstance());
  }

  private static <H> JsonNode toJsonNode(H value) {
    return ConfigurationUtils.jsonMapper().convertValue(value, JsonNode.class);
  }

  public String name() {
    return name;
  }

  public @NewInstance ConfigParamTestSpec<T> required(boolean required) {
    return new ConfigParamTestSpec<>(
        configType, name, required, typeParam, validValues, invalidValues, getter);
  }

  public boolean required() {
    return required;
  }

  public @NewInstance <H> ConfigParamTestSpec<T> validValues(H... validValues) {
    Collection<JsonNode> newValidValues = new ArrayList<>(this.invalidValues);
    Arrays.stream(validValues).map(ConfigParamTestSpec::toJsonNode).forEach(newValidValues::add);
    return new ConfigParamTestSpec<>(
        configType, name, required, typeParam, newValidValues, invalidValues, getter);
  }

  public Collection<JsonNode> validValues() {
    return validValues;
  }

  public @NewInstance <H> ConfigParamTestSpec<T> invalidValues(H... invalidValues) {
    Collection<JsonNode> newInvalidValues = new ArrayList<>(this.invalidValues);
    Arrays.stream(invalidValues)
        .map(ConfigParamTestSpec::toJsonNode)
        .forEach(newInvalidValues::add);
    return new ConfigParamTestSpec<>(
        configType, name, required, typeParam, validValues, newInvalidValues, getter);
  }

  public Collection<JsonNode> invalidValues() {
    return invalidValues;
  }

  public @NewInstance ConfigParamTestSpec<T> getter(Function<T, ?> getter) {
    return new ConfigParamTestSpec<>(
        configType, name, required, typeParam, validValues, invalidValues, getter);
  }

  public JsonNode get(T config) {
    return toJsonNode(getter.apply(config));
  }
}
