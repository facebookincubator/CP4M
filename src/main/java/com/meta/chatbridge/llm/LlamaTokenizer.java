/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.meta.chatbridge.store.MessageStack;
import com.meta.chatbridge.message.Message;

import java.util.*;

public class LlamaTokenizer {

    private final int MAX_TOKENS = 4000; // We subtract 96 to be safe when accounting for different counting of tokens between models
    private final int MAX_RESPONSE_TOKENS = 256;
    private final int MAX_CONTEXT_TOKENS = 1536;

    private final Encoding tokenizer;

    public LlamaTokenizer() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        tokenizer = registry.getEncoding(EncodingType.CL100K_BASE);
    }


    /**
     * Returns a count of the tokens from the context string
     *
     * @param contextString The context string to set LLM behavior
     */
    private int getContextStringTokens(String contextString) {
        var tokenCount = 0;

        tokenCount += tokenizer.encode(contextString + "\n---\n").size(); // Set the rest of the message here as system, etc

        return tokenCount;
    }

    /**
     * Removes old messages from the history until the total number of tokens + MAX_RESPONSE_TOKENS stays under MAX_TOKENS
     *
     * @param context        The "system message" context string, possibly needs to be updated to be more complicated and different across Llama and ChatGPT
     * @param history        The history of messages. The last message is the user question, do not remove it.
     * @return The capped messages that can be sent to the Llama endpoint.
     */
    public List<Message> getCappedMessages(String context,
                                           MessageStack history) {
        var availableTokens = MAX_TOKENS - MAX_RESPONSE_TOKENS - getContextStringTokens(context);
        var cappedHistory = new ArrayList<>(history.messages());

        var tokens = getTokenCount(cappedHistory);

        while (tokens > availableTokens) {
            if (cappedHistory.size() == 1) {
                throw new RuntimeException("Cannot cap messages further, only user question left");
            }

            cappedHistory.remove(0);
            tokens = getTokenCount(cappedHistory);
        }


        var cappedMessages = new ArrayList<Message>(cappedHistory); //Possibly change to include system message?

        return cappedMessages;
    }

    /**
     * Returns the number of tokens in the list of messages.
     *
     * @param messages The messages to count the tokens of
     * @return The number of tokens in the messages
     */
    private int getTokenCount(List<Message> messages) {
        var tokenCount = 3; // Update to account for the amount of always present filler tokens
        for (var message : messages) {
            tokenCount += getMessageTokenCount(message);
        }
        return tokenCount;
    }

    /**
     * Returns the number of tokens in the message.
     *
     * @param message The message to count the tokens of
     * @return The number of tokens in the message
     */
    private int getMessageTokenCount(Message message) {
        var tokens = 4; // Also figure out the basic token overhead here

        tokens += tokenizer.encode(message.role().toString()).size();
        tokens += tokenizer.encode(message.message()).size();

        return tokens;
    }
}
