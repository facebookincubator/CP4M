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

class NullStoreStoreTest {

    @Test
    void test() {
        Identifier senderId = Identifier.random();
        Identifier recipientId = Identifier.random();

        MessageFactory<FBMessage> messageFactory = MessageFactory.instance(FBMessage.class);
        NullStore<FBMessage> nullStore = new NullStore<>();

        assertThat(nullStore.size()).isEqualTo(0);
    FBMessage message =
        messageFactory.newMessage(
            Instant.now(),
            new Payload.Text(""),
            senderId,
            recipientId,
            Identifier.random(),
            Message.Role.ASSISTANT);
        ThreadState<FBMessage> thread = nullStore.add(message);
        assertThat(nullStore.size()).isEqualTo(0);
        assertThat(thread.messages()).hasSize(1).contains(message);

    FBMessage message2 =
        messageFactory.newMessage(
            Instant.now(),
            new Payload.Text(""),
            recipientId,
            senderId,
            Identifier.random(),
            Message.Role.USER);
        thread = nullStore.add(message2);
        assertThat(nullStore.size()).isEqualTo(0);
        assertThat(thread.messages()).hasSize(1);

    FBMessage message3 =
        messageFactory.newMessage(
            Instant.now(),
            new Payload.Text(""),
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
        thread = nullStore.add(message3);
        assertThat(nullStore.size()).isEqualTo(0);
        assertThat(thread.messages()).hasSize(1).contains(message3);
    }
}
