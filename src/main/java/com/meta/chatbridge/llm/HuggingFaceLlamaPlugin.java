/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.message.MessageStack;
import kotlin.Pair;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HuggingFaceLlamaPlugin<T extends Message> implements LLMPlugin<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HuggingFaceConfig config;
    private URI endpoint;

    public HuggingFaceLlamaPlugin(HuggingFaceConfig config) {
        this.config = config;

        try {
            this.endpoint = new URI(this.config.endpoint());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e); // this should be impossible
        }
    }

    @TestOnly
    public @This HuggingFaceLlamaPlugin<T> endpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    private int tokenCount(JsonNode message) {
//        int tokenCount = tokensPerMessage;
//        tokenCount += tokenEncoding.countTokens(message.get("content").textValue());
//        tokenCount += tokenEncoding.countTokens(message.get("role").textValue());
//        @Nullable JsonNode name = message.get("name");
//        if (name != null) {
//            tokenCount += tokenEncoding.countTokens(name.textValue());
//            tokenCount += tokensPerName;
//        }
//        return tokenCount;
        return 100;
    }

    private Optional<ArrayNode> pruneMessages(ArrayNode messages, @Nullable JsonNode functions)
            throws JsonProcessingException {

        ArrayNode output = MAPPER.createArrayNode();
        int totalTokens = 0;
        totalTokens += 3; // every reply is primed with <|start|>assistant<|message|>

        JsonNode systemMessage = messages.get(0);
        boolean hasSystemMessage = systemMessage.get("role").textValue().equals("system");
        if (hasSystemMessage) {
            // if the system message is present it's required
            totalTokens += tokenCount(messages.get(0));
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            JsonNode m = messages.get(i);
            String role = m.get("role").textValue();
            if (role.equals("system")) {
                continue; // system has already been counted
            }
            totalTokens += tokenCount(m);
            if (totalTokens > config.maxInputTokens()) {
                break;
            }
            output.insert(0, m);
        }
        if (hasSystemMessage) {
            output.insert(0, systemMessage);
        }

        if ((hasSystemMessage && output.size() <= 1) || output.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(output);
    }

    @Override
    public T handle(MessageStack<T> messageStack) throws IOException {
        T fromUser = messageStack.tail();

        ObjectNode body = MAPPER.createObjectNode();
        ObjectNode params = MAPPER.createObjectNode();

        config.topP().ifPresent(v -> params.put("top_p", v));
        config.temperature().ifPresent(v -> params.put("temperature", v));
        config.maxOutputTokens().ifPresent(v -> params.put("max_new_tokens", v));

        body.set("parameters", params);

        String prompt = getPrompt(messageStack);

        body.put("inputs", prompt);

        String bodyString;
        try {
            bodyString = MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e); // this should be impossible
        }
        Response response =
                Request.post(endpoint)
                        .bodyString(bodyString, ContentType.APPLICATION_JSON)
                        .setHeader("Authorization", "Bearer " + config.apiKey())
                        .execute();

        JsonNode responseBody = MAPPER.readTree(response.returnContent().asBytes());
        String allGeneratedText = responseBody.get(0).get("generated_text").textValue();
        String llmResponse = allGeneratedText.trim().replace(prompt.trim(), "");
        Instant timestamp = Instant.now();

        return messageStack.newMessageFromBot(timestamp, llmResponse);
    }

    private String getPrompt(MessageStack<T> MessageStack) {
        List<String> texts = new ArrayList<>();
        if(config.systemMessage().isPresent()){
            texts.add("<s>[INST] <<SYS>>\n" + config.systemMessage() + "\n<</SYS>>\n\n");
        } else {
            texts.add("<s>[INST] ");
        }

        // The first user input is _not_ stripped
        boolean doStrip = false;
        Message.Role lastMessageSender = Message.Role.SYSTEM;

        for (T message : MessageStack.messages()) {
            String text = doStrip ?  message.message().trim() : message.message();
            Message.Role user = message.role();
            boolean isUser = user.equals(Message.Role.USER);
            if(isUser){
                doStrip = true;
            }

            if(isUser && lastMessageSender.equals(Message.Role.ASSISTANT)){
                texts.add(" </s><s>[INST] ");
            }
            if(user.equals(Message.Role.ASSISTANT) && lastMessageSender.equals(Message.Role.USER)){
                texts.add(" [/INST] ");
            }
            texts.add(text);

            lastMessageSender = user;
        }
        if(lastMessageSender.equals(Message.Role.ASSISTANT)){
            texts.add(" </s>");
        } else if (lastMessageSender.equals(Message.Role.USER)){
            texts.add(" [/INST]");
        }

        StringBuilder promptBuilder = new StringBuilder();
        for (String text : texts) {
            promptBuilder.append(text);
        }

        return promptBuilder.toString();
    }
}
