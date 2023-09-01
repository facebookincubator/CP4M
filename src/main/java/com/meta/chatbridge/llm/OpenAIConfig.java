/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;

@JsonDeserialize(builder = OpenAIConfig.Builder.class)
public class OpenAIConfig {

  private final OpenAIModel model;
  private final String apiKey;
  @Nullable private final Double temperature;
  @Nullable private final Double topP;
  @Nullable private final Long maxTokens;
  @Nullable private final Double presencePenalty;
  @Nullable private final Double frequencyPenalty;
  private final Map<Long, Double> logitBias;
  private final @Nullable String systemMessage;

  private OpenAIConfig(
      OpenAIModel model,
      String apiKey,
      @Nullable Double temperature,
      @Nullable Double topP,
      @Nullable Long maxTokens,
      @Nullable Double presencePenalty,
      @Nullable Double frequencyPenalty,
      Map<Long, Double> logitBias,
      @Nullable String systemMessage) {
    this.apiKey = apiKey;
    this.temperature = temperature;
    this.topP = topP;
    this.model = model;
    this.maxTokens = maxTokens;
    this.presencePenalty = presencePenalty;
    this.frequencyPenalty = frequencyPenalty;
    this.logitBias = Collections.unmodifiableMap(logitBias);
    this.systemMessage = systemMessage;
  }

  public static Builder builder(OpenAIModel model, String apiKey) {
    return new Builder().model(model).apiKey(apiKey);
  }

  public OpenAIModel model() {
    return model;
  }

  public String apiKey() {
    return apiKey;
  }

  public Optional<Double> temperature() {
    return Optional.ofNullable(temperature);
  }

  public Optional<Double> topP() {
    return Optional.ofNullable(topP);
  }

  public Optional<Long> maxTokens() {
    return Optional.ofNullable(maxTokens);
  }

  public Optional<Double> presencePenalty() {
    return Optional.ofNullable(presencePenalty);
  }

  public Optional<Double> frequencyPenalty() {
    return Optional.ofNullable(frequencyPenalty);
  }

  public Map<Long, Double> logitBias() {
    return logitBias;
  }

  public Optional<String> systemMessage() {
    return Optional.ofNullable(systemMessage);
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder {
    private @Nullable OpenAIModel model;

    @JsonProperty("api_key")
    private @Nullable String apiKey;

    private @Nullable Double temperature;

    @JsonProperty("top_p")
    private @Nullable Double topP;

    @JsonProperty("max_tokens")
    private @Nullable Long maxTokens;

    @JsonProperty("presence_penalty")
    private @Nullable Double presencePenalty;

    @JsonProperty("frequency_penalty")
    private @Nullable Double frequencyPenalty;

    @JsonProperty("logit_bias")
    private Map<Long, Double> logitBias = Collections.emptyMap();

    @JsonProperty("system_message")
    private @Nullable String systemMessage;

    public @This Builder model(OpenAIModel model) {
      this.model = model;
      return this;
    }

    public @This Builder apiKey(String apiKey) {
      Preconditions.checkArgument(!apiKey.isBlank(), "api key cannot be empty");
      this.apiKey = apiKey;
      return this;
    }

    public @This Builder temperature(double temperature) {
      Preconditions.checkArgument(
          temperature >= 0 && temperature <= 2, "temperature must be >= 0 and <= 2");
      this.temperature = temperature;
      return this;
    }

    public @This Builder topP(double topP) {
      Preconditions.checkArgument(topP > 0 && topP <= 1, "top_p must be > 0 and <= 1");
      this.topP = topP;
      return this;
    }

    public @This Builder maxTokens(long maxTokens) {
      Preconditions.checkArgument(maxTokens > 0, "max_tokens must be greater than zero");
      this.maxTokens = maxTokens;
      return this;
    }

    public @This Builder presencePenalty(double presencePenalty) {
      Preconditions.checkArgument(
          presencePenalty >= -2.0 && presencePenalty <= 2.0,
          "presence_penalty must be between -2.0 and 2.0");
      this.presencePenalty = presencePenalty;
      return this;
    }

    public @This Builder frequencyPenalty(double frequencyPenalty) {
      Preconditions.checkArgument(
          frequencyPenalty >= -2.0 && frequencyPenalty <= 2.0,
          "frequency_penalty must be between -2.0 and 2.0");
      this.frequencyPenalty = frequencyPenalty;
      return this;
    }

    public @This Builder logitBias(Map<Long, Double> logitBias) {
      Preconditions.checkArgument(
          logitBias.values().stream().allMatch(v -> v >= -100 && v <= 100),
          "all values for log_bias must be between -100 and 100");
      this.logitBias = logitBias;
      return this;
    }

    public @This Builder systemMessage(String systemMessage) {
      Preconditions.checkArgument(!systemMessage.isBlank(), "system_message cannot be blank");
      this.systemMessage = systemMessage;
      return this;
    }

    public OpenAIConfig build() {
      Objects.requireNonNull(model, "model is a required parameter");
      Objects.requireNonNull(apiKey, "api_key is a required parameter");
      if (maxTokens != null) {
        Preconditions.checkArgument(
            maxTokens <= model.properties().tokenLimit(),
            "max_tokens must be <= "
                + model.properties().tokenLimit()
                + ", the maximum tokens allowed for the selected model '"
                + model.properties().name()
                + "'");
      }

      return new OpenAIConfig(
          model,
          apiKey,
          temperature,
          topP,
          maxTokens,
          presencePenalty,
          frequencyPenalty,
          logitBias,
          systemMessage);
    }
  }
}
