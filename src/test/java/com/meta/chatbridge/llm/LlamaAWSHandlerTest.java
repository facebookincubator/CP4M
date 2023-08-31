/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.meta.chatbridge.Identifier;
import com.meta.chatbridge.message.FBMessage;
import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.store.LLMContextManager;
import com.meta.chatbridge.store.MessageStack;

import java.time.Instant;

import software.amazon.awssdk.regions.Region;

public class LlamaAWSHandlerTest {

    public static void main(String[] args) {
        Region region = Region.US_EAST_2;
        String endpoint = "jumpstart-dft-meta-textgeneration-llama-2-7b-f";
        LLMContextManager context = new LLMContextManager("Always answer with emojis");

        LlamaAWSHandler llmhandler = new LlamaAWSHandler(context, endpoint, region);

        FBMessage userMessage = new FBMessage(
                Instant.now(),
                Identifier.from("123"),
                Identifier.from("456"),
                Identifier.from("789"),
                "How to go from San Francisco to NY?",
                Message.Role.USER
        );
        MessageStack<FBMessage> messageStack = MessageStack.of(userMessage);

        Message response = llmhandler.handle(messageStack);
        System.out.println(response.message());
    }
}
