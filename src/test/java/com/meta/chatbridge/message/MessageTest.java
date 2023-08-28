/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.meta.chatbridge.Identifier;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MessageTest {

  @Test
  void conversationId() {
    Instant timestamp = Instant.now();
    Identifier id0 = Identifier.from("0");
    Identifier id1 = Identifier.from("1");
    Identifier id2 = Identifier.from("2");
    Message message = new FBMessage(timestamp, id0, id1, id2, "", Message.Role.ASSISTANT);
    Message response = new FBMessage(timestamp, id0, id2, id1, "", Message.Role.ASSISTANT);
    assertThat(message.conversationId()).isEqualTo(response.conversationId());

    message =
        new FBMessage(
            timestamp,
            id0,
            Identifier.from("12"),
            Identifier.from("34"),
            "",
            Message.Role.ASSISTANT);
    response =
        new FBMessage(
            timestamp,
            id0,
            Identifier.from("1"),
            Identifier.from("234"),
            "",
            Message.Role.ASSISTANT);
    assertThat(message.conversationId()).isNotEqualTo(response.conversationId());
  }
}
