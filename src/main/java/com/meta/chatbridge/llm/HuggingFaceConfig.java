/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.meta.chatbridge.message.Message;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

@JsonDeserialize(builder = HuggingFaceConfig.Builder.class)
public class HuggingFaceConfig implements LLMConfig {

    private final String name;
    private final String apiKey;
    @Nullable private final Double temperature;
    @Nullable private final Double topP;
    private final List<String> stop;
    @Nullable private final Long maxOutputTokens;
    @Nullable private final Double presencePenalty;
    @Nullable private final Double frequencyPenalty;
    private final Map<Long, Double> logitBias;
    private final @Nullable String systemMessage;

    private final long maxInputTokens;

    private HuggingFaceConfig(
            String name,
            String apiKey,
            @Nullable Double temperature,
            @Nullable Double topP,
            List<String> stop,
            @Nullable Long maxOutputTokens,
            @Nullable Double presencePenalty,
            @Nullable Double frequencyPenalty,
            Map<Long, Double> logitBias,
            @Nullable String systemMessage,
            long maxInputTokens) {
        this.name = name;
        this.apiKey = apiKey;
        this.temperature = temperature;
        this.topP = topP;
        this.stop = stop;
        this.maxOutputTokens = maxOutputTokens;
        this.presencePenalty = presencePenalty;
        this.frequencyPenalty = frequencyPenalty;
        this.logitBias = Collections.unmodifiableMap(logitBias);
        this.systemMessage = systemMessage;
        this.maxInputTokens = maxInputTokens;
    }

    public static Builder builder(String apiKey) {
        // readability of the name is not important unless it comes from the config
        return new Builder().name(UUID.randomUUID().toString()).apiKey(apiKey);
    }

    public String name() {
        return name;
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

    public Collection<String> stop() {
        return Collections.unmodifiableCollection(stop);
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

    public <T extends Message> HuggingFacePlugin<T> toPlugin() {
        return new HuggingFacePlugin<>(this);
    }

    @Override
    public String name() {
        return null;
    }
}
