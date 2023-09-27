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
//    private final Encoding tokenEncoding;
//    private final int tokensPerMessage;
//    private final int tokensPerName;
    private URI endpoint;

    public HuggingFaceLlamaPlugin(HuggingFaceConfig config) {
        this.config = config;

        try {
            this.endpoint = new URI(this.config.endpoint());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e); // this should be impossible
        }
//        tokenEncoding =
//                Encodings.newDefaultEncodingRegistry()
//                        .getEncodingForModel(config.model().properties().jtokkinModel());
//
//        switch (config.model()) {
//            case GPT4, GPT432K -> {
//                tokensPerMessage = 3;
//                tokensPerName = 1;
//            }
//            case GPT35TURBO, GPT35TURBO16K -> {
//                tokensPerMessage = 4; // every message follows <|start|>{role/name}\n{content}<|end|>\n
//                tokensPerName = -1; // if there's a name, the role is omitted
//            }
//            default -> throw new IllegalArgumentException("Unsupported model: " + config.model());
//        }
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

//        int functionTokens = 0;
//        if (functions != null) {
//            // This is honestly a guess, it's undocumented
//            functionTokens = tokenEncoding.countTokens(MAPPER.writeValueAsString(functions));
//        }

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


//        body.put("model", config.model().properties().name())
//                // .put("function_call", "auto") // Update when we support functions
//                .put("n", 1)
//                .put("stream", false)
//                .put("user", fromUser.senderId().toString());
        config.topP().ifPresent(v -> params.put("top_p", v));
        config.temperature().ifPresent(v -> params.put("temperature", v));
        config.maxOutputTokens().ifPresent(v -> params.put("max_new_tokens", v));
//        config.presencePenalty().ifPresent(v -> body.put("presence_penalty", v));
//        config.frequencyPenalty().ifPresent(v -> body.put("frequency_penalty", v));
//        if (!config.logitBias().isEmpty()) {
//            body.set("logit_bias", MAPPER.valueToTree(config.logitBias()));
//        }
//        if (!config.stop().isEmpty()) {
//            body.set("stop", MAPPER.valueToTree(config.stop()));
//        }

        body.set("parameters", params);

//        String payload = "test";
        String prompt = getPrompt(messageStack);


//        if (prunedMessages.isEmpty()) {
//            return messageStack.newMessageFromBot(
//                    Instant.now(), "I'm sorry but that request was too long for me.");
//        }
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

//        JsonNode choice = responseBody.get("choices").get(0);
//        String messageContent = choice.get("message").get("content").textValue();

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
//            Check if it's user or system and add tokens and text accordingly
//            If system and previous is user, [/INST] " + response
//            If user and previous is system, </s><s>[INST] + message
//            Then end with either one but close brackets, either end with an open
//            inst or with a user message and no closing
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

//            String response = entry.getValue().trim();
//            texts.add(userInput + " [/INST] " + response + " </s><s>[INST] ");
        }
        if(lastMessageSender.equals(Message.Role.ASSISTANT)){
            texts.add(" </s>");
        } else if (lastMessageSender.equals(Message.Role.USER)){
            texts.add(" [/INST]");
        }

//        userMessage = doStrip ? userMessage.trim() : userMessage;
//        texts.add(userMessage + " [/INST]");

        StringBuilder promptBuilder = new StringBuilder();
        for (String text : texts) {
            promptBuilder.append(text);
        }

        return promptBuilder.toString();
    }
}
