/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.FBMessage;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.MessageFactory;
import com.meta.cp4m.message.ThreadState;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import org.checkerframework.common.returnsreceiver.qual.This;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HuggingFaceLlamaPrompt<T extends Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HuggingFaceLlamaPrompt.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String systemMessage;
    private final long maxInputTokens;
    private final HuggingFaceTokenizer tokenizer;

    public HuggingFaceLlamaPrompt(HuggingFaceConfig config) {

        this.systemMessage = config.systemMessage();
        this.maxInputTokens = config.maxInputTokens();
        URL llamaTokenizerUrl =
                Objects.requireNonNull(
                        HuggingFaceLlamaPrompt.class.getClassLoader().getResource("llamaTokenizer.json"));
        URI llamaTokenizer;
        try {
            llamaTokenizer = llamaTokenizerUrl.toURI();
            tokenizer = HuggingFaceTokenizer.newInstance(Paths.get(llamaTokenizer));

        } catch (URISyntaxException | IOException e) {
            // this should be impossible
            throw new RuntimeException(e);
        }
    }

    public String createPrompt(ThreadState<T> threadState) {

        PromptBuilder builder = new PromptBuilder();

//        First we prune messages and count, then we do this promptbuilder with the remaining messages


        int totalTokens = 5; // Account for closing tokens
        Message systemMessage = threadState.messages().get(0).role().equals(Message.Role.SYSTEM) ? threadState.messages().get(0) : MessageFactory.instance(FBMessage.class)
                .newMessage(
                        Instant.now(),
                        this.systemMessage,
                        Identifier.random(),
                        Identifier.random(),
                        Identifier.random(),
                        Message.Role.SYSTEM);
        ArrayList<Message> output = new ArrayList<>();
        totalTokens += tokenCount(systemMessage.message());
        for (int i = threadState.messages().size() - 1; i >= 0; i--) {
            Message m = threadState.messages().get(i);

            if (m.role().equals(Message.Role.SYSTEM)) {
                continue; // system has already been counted
            }
            totalTokens += tokenCount(m.message());
            if (totalTokens > maxInputTokens) {
                break;
            }
            output.add(0, m);
        }
        if (output.isEmpty()) {
            return "I'm sorry but that request was too long for me.";
        }
        output.add(0, systemMessage);

        for (Message message : output) {
            switch (message.role()) {
                case SYSTEM -> builder.addSystem(message);
                case USER -> builder.addUser(message);
                case ASSISTANT -> builder.addAssistant(message);
            }
        }

        return builder.build();
    }

    private int tokenCount(String message) {
        Encoding encoding = tokenizer.encode(message);
        return encoding.getTokens().length - 1;
    }

    // TODO: move logic into promptbuilder
    private String pruneMessages(ThreadState<T> threadState) {

        int totalTokens = 5; // Account for closing tokens at end of message
        StringBuilder promptStringBuilder = new StringBuilder();
        String systemPrompt = "<s>[INST] <<SYS>>\n" + systemMessage + "\n<</SYS>>\n\n";
        totalTokens += tokenCount(systemPrompt);
        promptStringBuilder
                .append("<s>[INST] <<SYS>>\n")
                .append(systemMessage)
                .append("\n<</SYS>>\n\n");

        Message.Role nextMessageSender = Message.Role.ASSISTANT;
        StringBuilder contextStringBuilder = new StringBuilder();

        List<T> messages = threadState.messages();

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            StringBuilder messageText = new StringBuilder();
            String text = message.message().strip();
            Message.Role user = message.role();
            boolean isUser = user == Message.Role.USER;
            messageText.append(text);
            if (isUser && nextMessageSender == Message.Role.ASSISTANT) {
                messageText.append(" [/INST] ");
            } else if (user == Message.Role.ASSISTANT && nextMessageSender == Message.Role.USER) {
                messageText.append(" </s><s>[INST] ");
            }
            totalTokens += tokenCount(messageText.toString());
            if (totalTokens > maxInputTokens) {
                if (contextStringBuilder.isEmpty()) {
                    return "I'm sorry but that request was too long for me.";
                }
                break;
            }
            contextStringBuilder.append(messageText.reverse());

            nextMessageSender = user;
        }
        if (nextMessageSender == Message.Role.ASSISTANT) {
            contextStringBuilder.append(
                    " ]TSNI/[ "); // Reversed [/INST] to close instructions for when first message after
            // system prompt is not from user
        }

        promptStringBuilder.append(contextStringBuilder.reverse());
        return promptStringBuilder.toString().strip();
    }

    // TODO: convert this to a class and implement the methods to replace pruneMethod
    private class PromptBuilder {

        int totalTokens = 5;
        StringBuilder promptStringBuilder = new StringBuilder();

        void addSystem(Message message) {
            promptStringBuilder
                    .append("<s>[INST] <<SYS>>\n")
                    .append(message.message())
                    .append("\n<</SYS>>\n\n");
        }

        void addAssistant(Message message) {
            promptStringBuilder
                    .append(message.message())
                    .append(" </s><s>[INST] ");

        }

        void addUser(Message message) {
            promptStringBuilder
                    .append(message.message())
                    .append(" [/INST] ");

        }

        String build() {
            return promptStringBuilder.toString().strip();
        }
    }
}