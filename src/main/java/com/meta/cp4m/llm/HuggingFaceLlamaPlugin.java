/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;

public class HuggingFaceLlamaPlugin<T extends Message> implements LLMPlugin<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HuggingFaceConfig config;
    private final HuggingFaceLlamaPrompt<T> promptCreator;

    private URI endpoint;

    public HuggingFaceLlamaPlugin(HuggingFaceConfig config) {
        this.config = config;
        this.endpoint = this.config.endpoint();
        promptCreator = new HuggingFaceLlamaPrompt<>(config);
    }

    @Override
    public T handle(ThreadState<T> threadState) throws IOException {
        ObjectNode body = MAPPER.createObjectNode();
        ObjectNode params = MAPPER.createObjectNode();

        config.topP().ifPresent(v -> params.put("top_p", v));
        config.temperature().ifPresent(v -> params.put("temperature", v));
        config.maxOutputTokens().ifPresent(v -> params.put("max_new_tokens", v));

        body.set("parameters", params);

        String prompt = promptCreator.createPrompt(threadState);
        if (prompt.equals("I'm sorry but that request was too long for me.")) {
            return threadState.newMessageFromBot(Instant.now(), prompt);
        }

        body.put("inputs", prompt);

        String bodyString;
        try {
            bodyString = MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Response response =
                Request.post(endpoint)
                        .bodyString(bodyString, ContentType.APPLICATION_JSON)
                        .setHeader("Authorization", "Bearer " + config.apiKey())
                        .execute();

        JsonNode responseBody = MAPPER.readTree(response.returnContent().asBytes());
        String allGeneratedText = responseBody.get(0).get("generated_text").textValue();
        String llmResponse = allGeneratedText.strip().replace(prompt.strip(), "");
        Instant timestamp = Instant.now();

        return threadState.newMessageFromBot(timestamp, llmResponse);
    }
}