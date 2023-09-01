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
import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.message.Message.Role;
import com.meta.chatbridge.message.MessageStack;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.jetbrains.annotations.TestOnly;

public class OpenAIPlugin<T extends Message> implements LLMPlugin<T> {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
  private final OpenAIConfig config;

  private URI endpoint;

  public OpenAIPlugin(OpenAIConfig config) {
    this.config = config;

    try {
      this.endpoint = new URI(ENDPOINT);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e); // this should be impossible
    }
  }

  @This
  @TestOnly
  public OpenAIPlugin<T> endpoint(URI endpoint) {
    this.endpoint = endpoint;
    return this;
  }

  @Override
  public T handle(MessageStack<T> messageStack) throws IOException {
    T fromUser = messageStack.tail();

    ObjectNode body = MAPPER.createObjectNode();
    body.put("model", config.model().properties().name())
        // .put("function_call", "auto") // Update when we support functions
        .put("n", 1)
        .put("stream", false)
        .put("user", fromUser.senderId().toString());
    config.topP().ifPresent(v -> body.put("top_p", v));
    config.temperature().ifPresent(v -> body.put("temperature", v));
    config.maxTokens().ifPresent(v -> body.put("max_tokens", v));
    config.presencePenalty().ifPresent(v -> body.put("presence_penalty", v));
    config.frequencyPenalty().ifPresent(v -> body.put("frequency_penalty", v));
    if (!config.logitBias().isEmpty()) {
      body.set("logit_bias", MAPPER.valueToTree(config.logitBias()));
    }

    ArrayNode messages = body.putArray("messages");
    config
        .systemMessage()
        .ifPresent(
            m ->
                messages
                    .addObject()
                    .put("role", Role.SYSTEM.toString().toLowerCase())
                    .put("content", m));
    for (T message : messageStack.messages()) {
      messages
          .addObject()
          .put("role", message.role().toString().toLowerCase())
          .put("content", message.message());
    }

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
    Instant timestamp = Instant.ofEpochSecond(responseBody.get("created").longValue());
    JsonNode choice = responseBody.get("choices").get(0);
    String messageContent = choice.get("message").get("content").textValue();
    return messageStack.newMessageFromBot(timestamp, messageContent);
  }
}
