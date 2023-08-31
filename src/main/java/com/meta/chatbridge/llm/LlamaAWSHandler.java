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
import java.util.*;
import java.time.Instant;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointRequest;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointResponse;
import java.nio.charset.Charset;

public class LlamaAWSHandler implements LLMHandler {

    private final LLMContextManager context;
    private final LlamaTokenizer tokenizer;
    private final String endpoint;
    private final String contentType;
    private final Region region;

    public LlamaAWSHandler(LLMContextManager context, String endpoint, Region region) {
        this.context = context;
        this.tokenizer = new LlamaTokenizer();
        this.endpoint = endpoint;
        this.contentType = "application/json";
        this.region = region;
    }

    @Override
    public Message handle(MessageStack messageStack) {
        List<Message> messagesToPass = tokenizer.getCappedMessages(context.getContext(), messageStack);

        StringBuilder messagesString = new StringBuilder("{" + "\"inputs\": [[ { \"role\": \"system\", \"content\": \"" + context.getContext() + "\" },");
        for (Message m : messagesToPass){
            messagesString.append("{\"role\": \"").append(m.role().toString().toLowerCase()).append("\", \"content\": \"").append(m.message()).append("\"}");
        }

        String payload = messagesString + "]],\n\"parameters\": {\"max_new_tokens\": 256, \"top_p\": 0.9, \"temperature\": 0.6}}";

        SageMakerRuntimeClient runtimeClient = SageMakerRuntimeClient.builder()
                .region(region)
                .build();

        String responseString = invokeSpecificEndpoint(runtimeClient, endpoint, payload, contentType);
        Message last = messagesToPass.get(messagesToPass.size() - 1);
        Identifier tempMessageID = Identifier.from("-1");  // Replace with MID once sent by messageHandler

        Message response =
                new FBMessage(
                        Instant.now(),
                        tempMessageID,
                        last.recipientId(),
                        last.senderId(),
                        responseString,
                        Message.Role.ASSISTANT);

        return response;

    }

    public static String invokeSpecificEndpoint(SageMakerRuntimeClient runtimeClient, String endpointName, String payload, String contentType) {

        InvokeEndpointRequest endpointRequest = InvokeEndpointRequest.builder()
                .endpointName(endpointName)
                .contentType(contentType)
                .body(SdkBytes.fromString(payload, Charset.defaultCharset()))
                .customAttributes("accept_eula=true")
                .build();

        InvokeEndpointResponse response = runtimeClient.invokeEndpoint(endpointRequest);
        return (response.body().asString(Charset.defaultCharset()));
    }
}
