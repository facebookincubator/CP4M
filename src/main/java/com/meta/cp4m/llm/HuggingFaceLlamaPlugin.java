/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;

public class HuggingFaceLlamaPlugin<T extends Message> implements LLMPlugin<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HuggingFaceConfig config;
    private URI endpoint;

    public HuggingFaceLlamaPlugin(HuggingFaceConfig config) {
        this.config = config;
    this.endpoint = this.config.endpoint();
    }

  @Override
  public T handle(ThreadState<T> messageStack) throws IOException {
        T fromUser = messageStack.tail();

        ObjectNode body = MAPPER.createObjectNode();
        ObjectNode params = MAPPER.createObjectNode();

        config.topP().ifPresent(v -> params.put("top_p", v));
        config.temperature().ifPresent(v -> params.put("temperature", v));
        config.maxOutputTokens().ifPresent(v -> params.put("max_new_tokens", v));

        body.set("parameters", params);

        String prompt = createPrompt(messageStack);

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
        String llmResponse = allGeneratedText.strip().replace(prompt.strip(), "");
        Instant timestamp = Instant.now();

        return messageStack.newMessageFromBot(timestamp, llmResponse);
    }

  public String createPrompt(ThreadState<T> MessageStack) {
        StringBuilder promptBuilder = new StringBuilder();
        if(config.systemMessage().isPresent()){
            promptBuilder.append("<s>[INST] <<SYS>>\n").append(config.systemMessage().get()).append("\n<</SYS>>\n\n");
        } else if(MessageStack.messages().get(0).role() == Message.Role.SYSTEM){
            promptBuilder.append("<s>[INST] <<SYS>>\n").append(MessageStack.messages().get(0).message()).append("\n<</SYS>>\n\n");
        }
        else {
            promptBuilder.append("<s>[INST] ");
        }

        // The first user input is _not_ stripped
        boolean doStrip = false;
        Message.Role lastMessageSender = Message.Role.SYSTEM;

        for (T message : MessageStack.messages()) {
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
}
