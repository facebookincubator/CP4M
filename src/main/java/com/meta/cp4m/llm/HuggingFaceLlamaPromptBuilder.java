/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import ai.djl.huggingface.tokenizers.Encoding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;


public class HuggingFaceLlamaPromptBuilder<T extends Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HuggingFaceLlamaPromptBuilder.class);

    public String createPrompt(ThreadState<T> threadState, HuggingFaceConfig config) {

        URI resource = null;
        try {
            resource = Objects.requireNonNull(HuggingFaceLlamaPromptBuilder.class.getClassLoader().getResource("llamaTokenizer.json")).toURI();
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to find local llama tokenizer.json file", e);
        }

        try {
            assert resource != null;
            HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(Paths.get(resource));
            return pruneMessages(threadState, config, tokenizer);
        } catch (IOException e) {
            LOGGER.error("Failed to initialize Llama2 tokenizer from local file", e);
        }

        if(config.systemMessage().isPresent()){
            return "<s>[INST] <<SYS>>\n" + (config.systemMessage().get()) + "\n<</SYS>>\n\n" + threadState.messages().get(threadState.messages().size() - 1) + " [/INST] ";
        }
        else{
            return "<s>[INST] " + threadState.messages().get(threadState.messages().size() - 1) + " [/INST] ";
        }
    }

    private int tokenCount(String message, HuggingFaceTokenizer tokenizer) {
        Encoding encoding = tokenizer.encode(message);
        return encoding.getTokens().length - 1;
    }

    private String pruneMessages(ThreadState<T> threadState, HuggingFaceConfig config, HuggingFaceTokenizer tokenizer)
            throws JsonProcessingException {

        int totalTokens = 5; // Account for closing tokens at end of message
        StringBuilder promptBuilder = new StringBuilder();
        if(config.systemMessage().isPresent()){
            String systemPrompt = "<s>[INST] <<SYS>>\n" + config.systemMessage().get() + "\n<</SYS>>\n\n";
            totalTokens += tokenCount(systemPrompt, tokenizer);
            promptBuilder.append("<s>[INST] <<SYS>>\n").append(config.systemMessage().get()).append("\n<</SYS>>\n\n");
        } else if(threadState.messages().get(0).role() == Message.Role.SYSTEM){
            String systemPrompt = "<s>[INST] <<SYS>>\n" + threadState.messages().get(0).message() + "\n<</SYS>>\n\n";
            totalTokens += tokenCount(systemPrompt, tokenizer);
            promptBuilder.append("<s>[INST] <<SYS>>\n").append(threadState.messages().get(0).message()).append("\n<</SYS>>\n\n");
        }
        else {
            totalTokens += 6;
            promptBuilder.append("<s>[INST] ");
        }

        // The first user input is _not_ stripped
        boolean hasUserMessage = false;
        Message.Role lastMessageSender = Message.Role.SYSTEM;

        for (T message : threadState.messages()) {
            StringBuilder messageText = new StringBuilder();
            String text = hasUserMessage ?  message.message().strip() : message.message();
            Message.Role user = message.role();
            if (user == Message.Role.SYSTEM){
                continue;
            }
            boolean isUser = user == Message.Role.USER;

            if(isUser && lastMessageSender == Message.Role.ASSISTANT){
                messageText.append(" </s><s>[INST] ");
            }
            if(user == Message.Role.ASSISTANT && lastMessageSender == Message.Role.USER){
                messageText.append(" [/INST] ");
            }
            messageText.append(text);
            totalTokens += tokenCount(messageText.toString(), tokenizer);
            if(totalTokens > config.maxInputTokens()){
                if(!hasUserMessage){
                    return "I'm sorry but that request was too long for me.";
                }
                break;
            }
            promptBuilder.append(messageText);

            lastMessageSender = user;
            if(isUser){
                hasUserMessage = true;
            }
        }
        if(lastMessageSender == Message.Role.ASSISTANT){
            promptBuilder.append(" </s>");
        } else if (lastMessageSender == Message.Role.USER){
            promptBuilder.append(" [/INST]");
        }

        return promptBuilder.toString();
    }
}
