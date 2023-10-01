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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.message.MessageStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

public class HuggingFaceLlamaPromptBuilder<T extends Message> {

    private static final int MAX_TOTAL_TOKENS = 4096;
    private static final int MAX_INPUT_TOKENS = 4096;
//    private final Encoding tokenEncoding =
//            Encodings.newDefaultEncodingRegistry()
//            .getEncodingForModel(config.model().properties().jtokkinModel());
    public HuggingFaceLlamaPromptBuilder() {

    }
    public String createPrompt(MessageStack<T> messageStack, HuggingFaceConfig config){
        StringBuilder promptBuilder = new StringBuilder();
        if(config.systemMessage().isPresent()){
            promptBuilder.append("<s>[INST] <<SYS>>\n").append(config.systemMessage().get()).append("\n<</SYS>>\n\n");
        } else if(messageStack.messages().get(0).role() == Message.Role.SYSTEM){
            promptBuilder.append("<s>[INST] <<SYS>>\n").append(messageStack.messages().get(0).message()).append("\n<</SYS>>\n\n");
        }
        else {
            promptBuilder.append("<s>[INST] ");
        }

        // The first user input is _not_ stripped
        boolean doStrip = false;
        Message.Role lastMessageSender = Message.Role.SYSTEM;

        for (T message : messageStack.messages()) {
            String text = doStrip ?  message.message().strip() : message.message();
            Message.Role user = message.role();
            if (user == Message.Role.SYSTEM){
                continue;
            }
            boolean isUser = user == Message.Role.USER;
            if(isUser){
                doStrip = true;
            }

            if(isUser && lastMessageSender == Message.Role.ASSISTANT){
                promptBuilder.append(" </s><s>[INST] ");
            }
            if(user == Message.Role.ASSISTANT && lastMessageSender == Message.Role.USER){
                promptBuilder.append(" [/INST] ");
            }
            promptBuilder.append(text);

            lastMessageSender = user;
        }
        if(lastMessageSender == Message.Role.ASSISTANT){
            promptBuilder.append(" </s>");
        } else if (lastMessageSender == Message.Role.USER){
            promptBuilder.append(" [/INST]");
        }

        return promptBuilder.toString();
    }

    private int tokenCount(JsonNode message) {
        int tokenCount = tokensPerMessage;
        tokenCount += tokenEncoding.countTokens(message.get("content").textValue());
        tokenCount += tokenEncoding.countTokens(message.get("role").textValue());
        @Nullable JsonNode name = message.get("name");
        if (name != null) {
            tokenCount += tokenEncoding.countTokens(name.textValue());
            tokenCount += tokensPerName;
        }
        return tokenCount;
    }

    private Optional<ArrayNode> pruneMessages(ArrayNode messages, @Nullable JsonNode functions)
            throws JsonProcessingException {

        int functionTokens = 0;
        if (functions != null) {
            // This is honestly a guess, it's undocumented
            functionTokens = tokenEncoding.countTokens(MAPPER.writeValueAsString(functions));
        }

        ArrayNode output = MAPPER.createArrayNode();
        int totalTokens = functionTokens;
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
            if (totalTokens > MAX_TOTAL_TOKENS) {
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
}
