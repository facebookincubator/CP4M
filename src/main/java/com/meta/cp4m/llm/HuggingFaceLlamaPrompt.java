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

public class HuggingFaceLlamaPrompt<T extends Message> {

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

    public Optional<String> createPrompt(ThreadState<T> threadState) {

        PromptBuilder builder = new PromptBuilder();

//        int totalTokens = 5; // Account for closing tokens
//        Message systemMessage = threadState.messages().get(0).role().equals(Message.Role.SYSTEM) ? threadState.messages().get(0) : MessageFactory.instance(FBMessage.class)
//                .newMessage(
//                        Instant.now(),
//                        this.systemMessage,
//                        Identifier.random(),
//                        Identifier.random(),
//                        Identifier.random(),
//                        Message.Role.SYSTEM);
//        ArrayList<Message> output = new ArrayList<>();
//        totalTokens += tokenCount(systemMessage.message());
//        for (int i = threadState.messages().size() - 1; i >= 0; i--) {
//            Message m = threadState.messages().get(i);
//
//            if (m.role().equals(Message.Role.SYSTEM)) {
//                continue; // system has already been counted
//            }
//            totalTokens += tokenCount(m.message());
//            if (totalTokens > maxInputTokens) {
//                break;
//            }
//            output.add(0, m);
//        }
//        if (output.isEmpty()) {
//            return Optional.empty();
//        }
//        output.add(0, systemMessage);


        int totalTokens = tokenCount(this.systemMessage) + 5; // Account for closing tokens
        builder.addSystem(this.systemMessage);

        for (int i = threadState.messages().size() - 1; i >= 0; i--) {
            Message m = threadState.messages().get(i);
            totalTokens += tokenCount(m.message());
            if (totalTokens > maxInputTokens) {
                if (i == threadState.messages().size() - 1){
                    return Optional.empty();
                }
                break;
            }
            switch (m.role()) {
                case USER -> builder.addUser(m.message());
                case ASSISTANT -> builder.addAssistant(m.message());
            }
        }

        return Optional.of(builder.build());
    }

    private int tokenCount(String message) {
        Encoding encoding = tokenizer.encode(message);
        return encoding.getTokens().length;
    }

    private static class PromptBuilder {
        
        StringBuilder promptStringBuilder = new StringBuilder();

        void addSystem(String message) {
            promptStringBuilder
                    .append("<s>[INST] <<SYS>>\n")
                    .append(message)
                    .append("\n<</SYS>>\n\n");
        }

        void addAssistant(String message) {
            promptStringBuilder
                    .append(message)
                    .append(" </s><s>[INST] ");

        }

        void addUser(String message) {
            promptStringBuilder
                    .append(message)
                    .append(" [/INST] ");

        }

        String build() {
            return promptStringBuilder.toString().strip();
        }
    }
}