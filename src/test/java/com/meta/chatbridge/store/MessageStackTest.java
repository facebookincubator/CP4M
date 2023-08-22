/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.meta.chatbridge.FBID;
import com.meta.chatbridge.message.Message;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessageStackTest {

  record TestMessage(Instant timestamp) implements Message {

    @Override
    public String instanceId() {
      return "0";
    }

    @Override
    public FBID senderId() {
      return FBID.from(0);
    }

    @Override
    public FBID recipientId() {
      return FBID.from(0);
    }

    @Override
    public String message() {
      return "";
    }

    @Override
    public Role role() {
      return Role.SYSTEM;
    }
  }

  @Test
  void orderPreservation() {
    Instant start = Instant.now();
    TestMessage message1 = new TestMessage(start);
    TestMessage message2 = new TestMessage(start.plusSeconds(1));
    MessageStack<TestMessage> ms = MessageStack.of(Lists.newArrayList(message1, message2));
    assertThat(ms.messages()).hasSize(2);
    assertThat(ms.messages()).first().isSameAs(message1);
    assertThat(ms.messages()).last().isSameAs(message2);

    ms = MessageStack.of(List.of());
    assertThat(ms.messages()).hasSize(0);
    ms = ms.with(message1);
    assertThat(ms.messages()).hasSize(1);
    ms = ms.with(message2);
    assertThat(ms.messages()).hasSize(2);
    assertThat(ms.messages()).first().isSameAs(message1);
    assertThat(ms.messages()).last().isSameAs(message2);
  }

  @Test
  void orderCorrection() {
    Instant start = Instant.now();
    TestMessage message1 = new TestMessage(start);
    TestMessage message2 = new TestMessage(start.plusSeconds(1));
    MessageStack<TestMessage> ms = MessageStack.of(Lists.newArrayList(message2, message1));
    assertThat(ms.messages()).first().isSameAs(message1);
    assertThat(ms.messages()).last().isSameAs(message2);

    ms = MessageStack.of(List.of());
    ms = ms.with(message2);
    assertThat(ms.messages()).hasSize(1);
    ms = ms.with(message1);
    assertThat(ms.messages()).hasSize(2);
    assertThat(ms.messages()).first().isSameAs(message1);
    assertThat(ms.messages()).last().isSameAs(message2);
  }
}
