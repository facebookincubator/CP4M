/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.meta.chatbridge.Identifier;
import com.meta.chatbridge.message.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemoryStoreTest {

  @Test
  void test() {
    Identifier senderId = Identifier.random();
    Identifier recipientId = Identifier.random();

    MessageFactory<FBMessage> messageFactory = MessageFactory.instance(FBMessage.class);
    MemoryStore<FBMessage> memoryStore = new MemoryStore<>(MemoryStoreConfig.of(1, 1));
    assertThat(memoryStore.size()).isEqualTo(0);
    FBMessage message =
        messageFactory.newMessage(
            Instant.now(), "", senderId, recipientId, Identifier.random(), Message.Role.SYSTEM);
    MessageStack<FBMessage> stack = memoryStore.add(message);
    assertThat(memoryStore.size()).isEqualTo(1);
    assertThat(stack.messages()).hasSize(1).contains(message);

    FBMessage message2 =
        messageFactory.newMessage(
            Instant.now(), "", recipientId, senderId, Identifier.random(), Message.Role.USER);
    stack = memoryStore.add(message2);
    assertThat(memoryStore.size()).isEqualTo(1);
    assertThat(stack.messages()).hasSize(2).contains(message, message2);

    FBMessage message3 =
        messageFactory.newMessage(
            Instant.now(),
            "",
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.SYSTEM);
    stack = memoryStore.add(message3);
    assertThat(memoryStore.size()).isEqualTo(2);
    assertThat(stack.messages()).hasSize(1).contains(message3);
  }
}
