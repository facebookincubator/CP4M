/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message.webhook.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class ErrorTest {
  private static final JsonMapper MAPPER = Utils.JSON_MAPPER;

  static String VALID =
      """
{
"code": 130429,
"title": "Rate limit hit",
"message": "(#130429) Rate limit hit",
"error_data": {"details": "Message failed to send because there were too many messages sent from this phone number in a short period of time."}
}
""";

  @Test
  void positive() throws JsonProcessingException {
    Error error = MAPPER.readValue(VALID, Error.class);
    assertThat(error)
        .isEqualTo(
            new Error(
                130429,
                "Rate limit hit",
                "(#130429) Rate limit hit",
                new Error.ErrorData(
                    "Message failed to send because there were too many messages sent from this phone number in a short period of time.")));
  }

  @Test
  void negative() throws JsonProcessingException {
    ObjectNode body = (ObjectNode) MAPPER.readTree(VALID);

    body.remove("code");
    assertThrows(
        MismatchedInputException.class,
        () -> MAPPER.readValue(MAPPER.writeValueAsString(body), Error.class));

    body.put("code", "hallo");
    assertThrows(
        MismatchedInputException.class,
        () -> MAPPER.readValue(MAPPER.writeValueAsString(body), Error.class));

    body.put("code", 130429);
    body.remove("title");
    assertThrows(
        ValueInstantiationException.class,
        () -> MAPPER.readValue(MAPPER.writeValueAsString(body), Error.class));
  }
}
