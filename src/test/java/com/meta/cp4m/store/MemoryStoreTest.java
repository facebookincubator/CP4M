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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MemoryStoreTest {

  @Test
  void test() {
    Identifier senderId = Identifier.random();
    Identifier recipientId = Identifier.random();

    MessageFactory<FBMessage> messageFactory = MessageFactory.instance(FBMessage.class);
    MemoryStore<FBMessage> memoryStore = new MemoryStore<>(MemoryStoreConfig.of(1, 1));
    assertThat(memoryStore.list().size()).isEqualTo(0);
    FBMessage message =
        messageFactory.newMessage(
            Instant.now(),
            new Payload.Text(""),
            senderId,
            recipientId,
            Identifier.random(),
            Message.Role.ASSISTANT);
    ThreadState<FBMessage> thread = memoryStore.add(message);
    assertThat(memoryStore.list().size()).isEqualTo(1);
    assertThat(thread.messages()).hasSize(1).contains(message);

    FBMessage message2 =
        messageFactory.newMessage(
            Instant.now(),
            new Payload.Text(""),
            recipientId,
            senderId,
            Identifier.random(),
            Message.Role.USER);
    thread = memoryStore.add(message2);
    assertThat(memoryStore.list().size()).isEqualTo(1);
    assertThat(thread.messages()).hasSize(2).contains(message, message2);

    FBMessage message3 =
        messageFactory.newMessage(
            Instant.now(),
            new Payload.Text(""),
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
    thread = memoryStore.add(message3);
    assertThat(memoryStore.list().size()).isEqualTo(2);
    assertThat(thread.messages()).hasSize(1).contains(message3);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 10})
  void historyLength(int numMessages) {
    Identifier senderId = Identifier.random();
    Identifier recipientId = Identifier.random();

    MessageFactory<FBMessage> messageFactory = MessageFactory.instance(FBMessage.class);
    MemoryStore<FBMessage> memoryStore = new MemoryStore<>(MemoryStoreConfig.of(1, 1, numMessages));
    assertThat(memoryStore.list().size()).isEqualTo(0);

    List<FBMessage> messages = new ArrayList<>((numMessages + 1) * 2);
    for (int mNum = 0; mNum < (numMessages + 1) * 2; mNum++) {
      FBMessage message =
          messageFactory.newMessage(
              Instant.now().plusSeconds(mNum),
              new Payload.Text(""),
              senderId,
              recipientId,
              Identifier.random(),
              Message.Role.ASSISTANT);
      messages.add(message);
      ThreadState<FBMessage> thread = memoryStore.add(message);
      assertThat(thread.messages())
          .hasSize(Math.min(mNum + 1, numMessages))
          //          .contains(message)
          .containsAll(
              messages.subList(messages.size() - Math.min(0, numMessages), messages.size()));
    }
  }
}
