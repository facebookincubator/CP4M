/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.store;

import static org.assertj.core.api.Assertions.*;

import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.FBMessage;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.MessageFactory;
import com.meta.cp4m.message.ThreadState;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ThreadStateTest {

  private static final MessageFactory<FBMessage> FACTORY = MessageFactory.instance(FBMessage.class);

  @Test
  void orderPreservation() {
    Instant start = Instant.now();
    FBMessage message1 =
        FACTORY.newMessage(
            start,
            "sample message",
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER, null);

    ThreadState<FBMessage> ms = ThreadState.of(message1);
    FBMessage message2 = ms.newMessageFromBot(start.plusSeconds(1), "other sample message", message1);
    ms = ms.with(message2);
    assertThat(ms.messages()).hasSize(2);
    assertThat(ms.messages().get(0)).isSameAs(message1);
    assertThat(ms.messages().get(1)).isSameAs(message2);

    ms = ThreadState.of(message1);
    assertThat(ms.messages()).hasSize(1);
    ms = ms.with(message2);
    assertThat(ms.messages()).hasSize(2);
    assertThat(ms.messages().get(0)).isSameAs(message1);
    assertThat(ms.messages().get(1)).isSameAs(message2);
  }

  @Test
  void orderCorrection() {
    Instant start = Instant.now();
    FBMessage message2 =
        FACTORY.newMessage(
            start,
            "sample message",
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER, null);
    ThreadState<FBMessage> ms = ThreadState.of(message2);

    FBMessage message1 = ms.newMessageFromBot(start.minusSeconds(1), "other sample message", message2);

    ms = ms.with(message1);
    assertThat(ms.messages().get(0)).isSameAs(message1);
    assertThat(ms.messages().get(1)).isSameAs(message2);

    ms = ThreadState.of(message2);
    assertThat(ms.messages()).hasSize(1);
    ms = ms.with(message1);
    assertThat(ms.messages()).hasSize(2);
    assertThat(ms.messages().get(0)).isSameAs(message1);
    assertThat(ms.messages().get(1)).isSameAs(message2);
  }

  @Test
  void botAndUserId() {
    Instant start = Instant.now();
    FBMessage message1 =
        FACTORY.newMessage(
            start,
            "sample message",
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER, null);

    ThreadState<FBMessage> ms = ThreadState.of(message1);
    FBMessage message2 =
        FACTORY.newMessage(
            start,
            "sample message",
            message1.recipientId(),
            message1.senderId(),
            Identifier.random(),
            Message.Role.ASSISTANT,
                message1);

    final ThreadState<FBMessage> finalMs = ms;
    assertThatCode(() -> finalMs.with(message2)).doesNotThrowAnyException();
    assertThatCode(() -> finalMs.with(finalMs.newMessageFromBot(start, "",message1)))
        .doesNotThrowAnyException();
    assertThatCode(() -> finalMs.with(finalMs.newMessageFromUser(start, "", Identifier.random())))
        .doesNotThrowAnyException();
    ms = ms.with(message2);
    assertThat(ms.userId()).isEqualTo(message1.senderId());
    assertThat(ms.botId()).isEqualTo(message1.recipientId());
    FBMessage mDifferentSenderId =
        FACTORY.newMessage(
            start,
            "",
            Identifier.random(),
            message1.recipientId(),
            Identifier.random(),
            Message.Role.USER, null);

    ThreadState<FBMessage> finalMs1 = ms;
    assertThatThrownBy(() -> finalMs1.with(mDifferentSenderId))
        .isInstanceOf(IllegalArgumentException.class);

    FBMessage mDifferentRecipientId =
        FACTORY.newMessage(
            start,
            "",
            message1.senderId(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER, null);
    assertThatThrownBy(() -> finalMs1.with(mDifferentRecipientId))
        .isInstanceOf(IllegalArgumentException.class);

    FBMessage illegalSenderId =
        FACTORY.newMessage(
            start,
            "",
            message1.recipientId(),
            message1.senderId(),
            Identifier.random(),
            Message.Role.USER, null);
    assertThatThrownBy(() -> finalMs1.with(illegalSenderId))
        .isInstanceOf(IllegalArgumentException.class);

    FBMessage illegalRecipientId =
        FACTORY.newMessage(
            start,
            "",
            message1.senderId(),
            message1.recipientId(),
            Identifier.random(),
            Message.Role.ASSISTANT, null);
    assertThatThrownBy(() -> finalMs1.with(illegalRecipientId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void systemMessageForbidden() {
    assertThatThrownBy(
            () ->
                ThreadState.of(
                    FACTORY.newMessage(
                        Instant.now(),
                        "",
                        Identifier.random(),
                        Identifier.random(),
                        Identifier.random(),
                        Message.Role.SYSTEM, null)))
        .isInstanceOf(IllegalArgumentException.class);

    ThreadState<FBMessage> threadState =
        ThreadState.of(
            FACTORY.newMessage(
                Instant.now(),
                "",
                Identifier.random(),
                Identifier.random(),
                Identifier.random(),
                Message.Role.USER, null));
    assertThatThrownBy(
            () ->
                threadState.with(
                    FACTORY.newMessage(
                        Instant.now(),
                        "",
                        Identifier.random(),
                        Identifier.random(),
                        Identifier.random(),
                        Message.Role.SYSTEM, null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void orderPreservationWhenUserSendsTwoMessagesInARow() {
    Instant start = Instant.now();
    Identifier senderId= Identifier.random();
    Identifier recipientId = Identifier.random();
    FBMessage userMessage1 =
            FACTORY.newMessage(
                    start,
                    "sample message 1",
                    senderId,
                    recipientId,
                    Identifier.random(),
                    Message.Role.USER, null);

    ThreadState<FBMessage> ms = ThreadState.of(userMessage1);
    FBMessage userMessage2 =
            FACTORY.newMessage(
                    start.plusSeconds(1),
                    "sample message 2",
                    senderId,
                    recipientId,
                    Identifier.random(),
                    Message.Role.USER, userMessage1);
    ms = ms.with(userMessage2);
    FBMessage botMessage1 = ms.newMessageFromBot(start.plusSeconds(4), "bot sample message 1", userMessage1);
    ms = ms.with(botMessage1);
    FBMessage botMessage2 = ms.newMessageFromBot(start.plusSeconds(8), "bot sample message 2", userMessage2);
    ms = ms.with(botMessage2);
    assertThat(ms.messages()).hasSize(4);
    assertThat(ms.messages().get(0).message()).isSameAs(userMessage1.message());
    assertThat(ms.messages().get(1).message()).isSameAs(botMessage1.message());
    assertThat(ms.messages().get(2).message()).isSameAs(userMessage2.message());
    assertThat(ms.messages().get(3).message()).isSameAs(botMessage2.message());
  }
}
