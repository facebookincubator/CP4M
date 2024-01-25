/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.*;
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
            Instant.now(), "", senderId, recipientId, Identifier.random(), Message.Role.ASSISTANT);
    ThreadState<FBMessage> thread = memoryStore.add(message);
    assertThat(memoryStore.size()).isEqualTo(1);
    assertThat(thread.messages()).hasSize(1).contains(message);

    FBMessage message2 =
        messageFactory.newMessage(
            Instant.now(), "", recipientId, senderId, Identifier.random(), Message.Role.USER);
    thread = memoryStore.add(message2);
    assertThat(memoryStore.size()).isEqualTo(1);
    assertThat(thread.messages()).hasSize(2);
    assertThat(thread.messages().get(0).instanceId()).isSameAs(message.instanceId());
    assertThat(thread.messages().get(1).instanceId()).isSameAs(message2.instanceId());

    FBMessage message3 =
        messageFactory.newMessage(
            Instant.now(),
            "",
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
    thread = memoryStore.add(message3);
    assertThat(memoryStore.size()).isEqualTo(2);
    assertThat(thread.messages()).hasSize(1).contains(message3);
  }
}
