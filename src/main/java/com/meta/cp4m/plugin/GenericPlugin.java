/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.Payload;
import com.meta.cp4m.message.ThreadState;
import com.meta.cp4m.message.UserData;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import org.apache.hc.core5.http.ContentType;

public class GenericPlugin<T extends Message> implements Plugin<T> {

  private static final JsonMapper MAPPER = new JsonMapper();
  private final URI url;
  private final AuthRequest authRequest;

  public GenericPlugin(URI url, AuthRequest authRequest) {
    this.url = url;
    this.authRequest = authRequest;
  }

  public GenericPlugin(URI url) {
    this(url, new AuthRequest.NoAuthRequest());
  }

  @Override
  public T handle(ThreadState<T> threadState) throws IOException {
    ObjectNode postPayload = MAPPER.createObjectNode();
    postPayload.put("timestamp", Instant.now().toString());
    ObjectNode userObj = postPayload.putObject("user");
    userObj.put("id", threadState.userId().toString());
    UserData userData = threadState.userData();
    userData.phoneNumber().ifPresent(pn -> userObj.put("phone_number", pn));
    userData.name().ifPresent(name -> userObj.put("name", name));
    ArrayNode messagesArray = postPayload.putArray("messages");
    for (T message : threadState.messages()) {
      if (message.payload() instanceof Payload.Text text) {
        messagesArray
            .addObject()
            .put("timestamp", message.timestamp().toString())
            .put("type", "text")
            .put("value", text.value());
      }
    }

    String jsonPostPayload;
    try {
      jsonPostPayload = MAPPER.writeValueAsString(postPayload);
    } catch (JsonProcessingException e) {
      // this should be impossible because we're building the payload here
      throw new RuntimeException(e);
    }

    GenericPluginThreadUpdateResponse response =
        authRequest
            .post(url)
            .bodyString(jsonPostPayload, ContentType.APPLICATION_JSON)
            .execute()
            .handleResponse(
                res ->
                    MAPPER.readValue(
                        res.getEntity().getContent(), GenericPluginThreadUpdateResponse.class));

    return threadState.newMessageFromBot(Instant.now(), response.value);
  }

  record GenericPluginThreadUpdateResponse(String type, String value) {

    @JsonCreator
    GenericPluginThreadUpdateResponse(
        @JsonProperty("type") String type, @JsonProperty("value") String value) {
      Preconditions.checkArgument(Objects.equals(type, "text"), "type must be equal to text");
      this.type = type;
      this.value = Objects.requireNonNull(value);
    }
  }
}
