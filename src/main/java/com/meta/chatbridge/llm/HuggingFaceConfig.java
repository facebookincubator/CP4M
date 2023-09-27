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
import com.meta.chatbridge.message.Message;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;

@JsonDeserialize(builder = HuggingFaceConfig.Builder.class)
public class HuggingFaceConfig implements LLMConfig {

    private final String endpoint;
    private final String name;
    private final String apiKey;
    @Nullable private final Double temperature;
    @Nullable private final Double topP;
    private final Long tokenLimit;
    @Nullable private final Long maxOutputTokens;
    @Nullable private final Double presencePenalty;
    @Nullable private final Double frequencyPenalty;
    private final Map<Long, Double> logitBias;
    private final @Nullable String systemMessage;

    private final long maxInputTokens;

    private HuggingFaceConfig(
            String endpoint,
            String name,
            String apiKey,
            @Nullable Double temperature,
            @Nullable Double topP,
            Long tokenLimit,
            @Nullable Long maxOutputTokens,
            @Nullable Double presencePenalty,
            @Nullable Double frequencyPenalty,
            Map<Long, Double> logitBias,
            @Nullable String systemMessage,
            long maxInputTokens) {
        this.endpoint = endpoint;
        this.name = name;
        this.apiKey = apiKey;
        this.temperature = temperature;
        this.topP = topP;
        this.tokenLimit = tokenLimit;
        this.maxOutputTokens = maxOutputTokens;
        this.presencePenalty = presencePenalty;
        this.frequencyPenalty = frequencyPenalty;
        this.logitBias = Collections.unmodifiableMap(logitBias);
        this.systemMessage = systemMessage;
        this.maxInputTokens = maxInputTokens;
    }

    private static void checkArgumentIsURI(String argument, String errorMessage) {
        try {
            new URI(argument);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    public static Builder builder(String apiKey) {
        // readability of the name is not important unless it comes from the config
        return new Builder().name(UUID.randomUUID().toString()).apiKey(apiKey);
    }

    public String endpoint() {
        return endpoint;
    }
    public String name() {
        return name;
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

    public Long tokenLimit() {
        return tokenLimit;
    }

    public Optional<Long> maxOutputTokens() {
        return Optional.ofNullable(maxOutputTokens);
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

    public long maxInputTokens() {
        return maxInputTokens;
    }

    public <T extends Message> HuggingFaceLlamaPlugin<T> toPlugin() {
        return new HuggingFaceLlamaPlugin<>(this);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private @Nullable String endpoint;
        private @Nullable String name;

        @JsonProperty("api_key")
        private @Nullable String apiKey;

        private @Nullable Double temperature;

        @JsonProperty("top_p")
        private @Nullable Double topP;

        @JsonProperty("token_limit")
        private @Nullable Long tokenLimit;

        @JsonProperty("max_output_tokens")
        private @Nullable Long maxOutputTokens;

        @JsonProperty("presence_penalty")
        private @Nullable Double presencePenalty;

        @JsonProperty("frequency_penalty")
        private @Nullable Double frequencyPenalty;

        @JsonProperty("logit_bias")
        private Map<Long, Double> logitBias = Collections.emptyMap();

        @JsonProperty("system_message")
        private @Nullable String systemMessage;

        @JsonProperty("max_input_tokens")
        private @Nullable Long maxInputTokens;

        public @This Builder endpoint(String endpoint) {
            checkArgumentIsURI(endpoint, "endpoint must b a valid URI");
            this.endpoint = endpoint;
            return this;
        }
        public @This Builder name(String name) {
            Preconditions.checkArgument(!name.isBlank(), "name cannot be blank");
            this.name = name;
            return this;
        }

        public @This Builder apiKey(String apiKey) {
            Objects.requireNonNull(apiKey);
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

        public @This Builder tokenLimit(long tokenLimit) {
            Preconditions.checkArgument(
                    tokenLimit > 0, "tokenLimit must be greater than zero");
            this.tokenLimit = tokenLimit;
            return this;
        }

        public @This Builder maxOutputTokens(long maxOutputTokens) {
            Preconditions.checkArgument(
                    maxOutputTokens > 0, "max_output_tokens must be greater than zero");
            this.maxOutputTokens = maxOutputTokens;
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

        public @This Builder maxInputTokens(long maxInputTokens) {
            Preconditions.checkArgument(maxInputTokens > 0, "max_input_tokens must be greater than zero");
            this.maxInputTokens = maxInputTokens;
            return this;
        }

        public HuggingFaceConfig build() {
            Objects.requireNonNull(endpoint, "endpoint is a required parameter");
            Objects.requireNonNull(name, "name is a required parameter");
            Objects.requireNonNull(tokenLimit, "token_limit is a required parameter");
            Objects.requireNonNull(apiKey, "api_key is a required parameter");
            if (maxOutputTokens != null) {
                Preconditions.checkArgument(
                        maxOutputTokens <= tokenLimit,
                        "max_tokens must be <= "
                                + tokenLimit
                                + "'");
            }
            if (maxInputTokens == null) {
                if (maxOutputTokens == null) {
                    // set the default max input size to 50% of the total context size so that there is always
                    // some room for the output
                    maxInputTokens = (long) (tokenLimit * 0.50);
                } else {
                    maxInputTokens = tokenLimit - maxOutputTokens;
                }
            }

            Preconditions.checkArgument(
                    maxInputTokens + (maxOutputTokens == null ? 0 : maxOutputTokens)
                            <= tokenLimit,
                    "max_input_tokens + max_output_tokens must total to be less than or equal to "
                            + tokenLimit
                            + ", the total context tokens allowed by this model");

            return new HuggingFaceConfig(
                    endpoint,
                    name,
                    apiKey,
                    temperature,
                    topP,
                    tokenLimit,
                    maxOutputTokens,
                    presencePenalty,
                    frequencyPenalty,
                    logitBias,
                    systemMessage,
                    maxInputTokens);
        }
    }
}
