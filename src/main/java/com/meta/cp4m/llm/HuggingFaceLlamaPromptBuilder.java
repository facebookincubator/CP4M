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

import java.beans.beancontext.BeanContextChild;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;


public class HuggingFaceLlamaPromptBuilder<T extends Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HuggingFaceLlamaPromptBuilder.class);

    public String createPrompt(ThreadState<T> threadState, HuggingFaceConfig config) {

//        NEW PLAN
//                WE do the node thing
//            and then we use the other token coutning thing where we add buffer tokens for each
//                And then we pass the remaining messages to the promptbuilder

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
        StringBuilder promptStringBuilder = new StringBuilder();
        if(config.systemMessage().isPresent()){
            String systemPrompt = "<s>[INST] <<SYS>>\n" + config.systemMessage().get() + "\n<</SYS>>\n\n";
            totalTokens += tokenCount(systemPrompt, tokenizer);
            promptStringBuilder.append("<s>[INST] <<SYS>>\n").append(config.systemMessage().get()).append("\n<</SYS>>\n\n");
        }
        else {
            totalTokens += 6;
            promptStringBuilder.append("<s>[INST] ");
        }

//        for (int i = list.size() - 1; i >= 0; i--)
//        {
//            // access elements by their index (position)
//            System.out.println(list.get(i));
//        }

//        Okay so we have a system prompt stringbuilder and then a context stringbuilder and we add those together and
//        only if context stringbuilde ris empty do we return the "too long" message



        // The first user input is _not_ stripped
//        boolean hasUserMessage = false;
        Message.Role nextMessageSender = Message.Role.ASSISTANT;
        StringBuilder contextStringBuilder = new StringBuilder();

        List<T> messages = threadState.messages();

        for (int i = messages.size() - 1; i >= 0; i--)
        {
            Message message = messages.get(i);
            StringBuilder messageText = new StringBuilder();
            String text = message.message().strip();
            Message.Role user = message.role();
            boolean isUser = user == Message.Role.USER;
            // access elements by their index (position)
            messageText.append(text);
            if (isUser && nextMessageSender == Message.Role.ASSISTANT){
                messageText.append(" [/INST] ");
            }
            else if (user == Message.Role.ASSISTANT && nextMessageSender == Message.Role.USER){
                messageText.append(" </s><s>[INST] ");
            }
            totalTokens += tokenCount(messageText.toString(), tokenizer);
            if(totalTokens > config.maxInputTokens()){
                if(contextStringBuilder.isEmpty()){
                    return "I'm sorry but that request was too long for me.";
                }
                break;
            }
            contextStringBuilder.append(messageText.reverse());

            nextMessageSender = user;
        }
        if(nextMessageSender == Message.Role.ASSISTANT){
            contextStringBuilder.append(" ]TSNI/[ "); // Reversed [/INST] to close instructions for when first message after system prompt is not from user
        }

        promptStringBuilder.append(contextStringBuilder.reverse());
        return promptStringBuilder.toString().strip();
    }
}
