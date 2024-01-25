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
import com.meta.cp4m.message.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ThreadStateTest {

  private static final MessageFactory<FBMessage> FB_FACTORY = MessageFactory.instance(FBMessage.class);
  private static final MessageFactory<WAMessage> WA_FACTORY = MessageFactory.instance(WAMessage.class);

  @Test
  void orderPreservation() {
    Instant start = Instant.now();
    FBMessage message1 =
        FB_FACTORY.newMessage(
            start,
            "sample message",
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);

    ThreadState<FBMessage> ms = ThreadState.of(message1);
    FBMessage message2 = ms.newMessageFromBot(start.plusSeconds(1), "other sample message");
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
        FB_FACTORY.newMessage(
            start,
            "sample message",
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
    ThreadState<FBMessage> ms = ThreadState.of(message2);

    FBMessage message1 = ms.newMessageFromBot(start.minusSeconds(1), "other sample message");

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
        FB_FACTORY.newMessage(
            start,
            "sample message",
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);

    ThreadState<FBMessage> ms = ThreadState.of(message1);
    FBMessage message2 =
        FB_FACTORY.newMessage(
            start,
            "sample message",
            message1.recipientId(),
            message1.senderId(),
            Identifier.random(),
            Message.Role.ASSISTANT);

    final ThreadState<FBMessage> finalMs = ms;
    assertThatCode(() -> finalMs.with(message2)).doesNotThrowAnyException();
    assertThatCode(() -> finalMs.with(finalMs.newMessageFromBot(start, "")))
        .doesNotThrowAnyException();
    assertThatCode(() -> finalMs.with(finalMs.newMessageFromUser(start, "", Identifier.random())))
        .doesNotThrowAnyException();
    ms = ms.with(message2);
    assertThat(ms.userId()).isEqualTo(message1.senderId());
    assertThat(ms.botId()).isEqualTo(message1.recipientId());
    FBMessage mDifferentSenderId =
        FB_FACTORY.newMessage(
            start,
            "",
            Identifier.random(),
            message1.recipientId(),
            Identifier.random(),
            Message.Role.USER);

    ThreadState<FBMessage> finalMs1 = ms;
    assertThatThrownBy(() -> finalMs1.with(mDifferentSenderId))
        .isInstanceOf(IllegalArgumentException.class);

    FBMessage mDifferentRecipientId =
        FB_FACTORY.newMessage(
            start,
            "",
            message1.senderId(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
    assertThatThrownBy(() -> finalMs1.with(mDifferentRecipientId))
        .isInstanceOf(IllegalArgumentException.class);

    FBMessage illegalSenderId =
        FB_FACTORY.newMessage(
            start,
            "",
            message1.recipientId(),
            message1.senderId(),
            Identifier.random(),
            Message.Role.USER);
    assertThatThrownBy(() -> finalMs1.with(illegalSenderId))
        .isInstanceOf(IllegalArgumentException.class);

    FBMessage illegalRecipientId =
        FB_FACTORY.newMessage(
            start,
            "",
            message1.senderId(),
            message1.recipientId(),
            Identifier.random(),
            Message.Role.ASSISTANT);
    assertThatThrownBy(() -> finalMs1.with((FBMessage) illegalRecipientId.withParentMessage(message1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void systemMessageForbidden() {
    assertThatThrownBy(
            () ->
                ThreadState.of(
                    FB_FACTORY.newMessage(
                        Instant.now(),
                        "",
                        Identifier.random(),
                        Identifier.random(),
                        Identifier.random(),
                        Message.Role.SYSTEM)))
        .isInstanceOf(IllegalArgumentException.class);

    ThreadState<FBMessage> threadState =
        ThreadState.of(
            FB_FACTORY.newMessage(
                Instant.now(),
                "",
                Identifier.random(),
                Identifier.random(),
                Identifier.random(),
                Message.Role.USER));
    assertThatThrownBy(
            () ->
                threadState.with(
                    FB_FACTORY.newMessage(
                        Instant.now(),
                        "",
                        Identifier.random(),
                        Identifier.random(),
                        Identifier.random(),
                        Message.Role.SYSTEM)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void orderPreservationWhenUserSendsTwoMessagesInARowFBMessage() {
    Instant start = Instant.now();
    Identifier senderId= Identifier.random();
    Identifier recipientId = Identifier.random();
    FBMessage userMessage1 =
            FB_FACTORY.newMessage(
                    start,
                    "sample message 1",
                    senderId,
                    recipientId,
                    Identifier.random(),
                    Message.Role.USER);
    ThreadState<FBMessage> ms = ThreadState.of(userMessage1);
    FBMessage userMessage2 =
            FB_FACTORY.newMessage(
                    start.plusSeconds(1),
                    "sample message 2",
                    senderId,
                    recipientId,
                    Identifier.random(),
                    Message.Role.USER);
    ThreadState<FBMessage> finalMs = ms;
    ms = ms.with(userMessage2);
    FBMessage botMessage1 = finalMs.newMessageFromBot(start.plusSeconds(4), "bot sample message 1");
    ms = ms.with(botMessage1);
    FBMessage botMessage2 = ms.newMessageFromBot(start.plusSeconds(8), "bot sample message 2");
    ms = ms.with(botMessage2);
    assertThat(ms.messages()).hasSize(4);
    assertThat(ms.messages().get(0).instanceId()).isSameAs(userMessage1.instanceId());
    assertThat(ms.messages().get(1).instanceId()).isSameAs(botMessage1.instanceId());
    assertThat(ms.messages().get(2).instanceId()).isSameAs(userMessage2.instanceId());
    assertThat(ms.messages().get(3).instanceId()).isSameAs(botMessage2.instanceId());
  }

  @Test
  void orderPreservationWhenUserSendsTwoMessagesInARowWAMessage() {
    Instant start = Instant.now();
    Identifier senderId= Identifier.random();
    Identifier recipientId = Identifier.random();
    WAMessage userMessage1 =
            WA_FACTORY.newMessage(
                    start,
                    "sample message 1",
                    senderId,
                    recipientId,
                    Identifier.random(),
                    Message.Role.USER);
    ThreadState<WAMessage> ms = ThreadState.of(userMessage1);
    WAMessage userMessage2 =
            WA_FACTORY.newMessage(
                    start.plusSeconds(1),
                    "sample message 2",
                    senderId,
                    recipientId,
                    Identifier.random(),
                    Message.Role.USER);
    ThreadState<WAMessage> finalMs = ms;
    ms = ms.with(userMessage2);
    WAMessage botMessage1 = finalMs.newMessageFromBot(start.plusSeconds(4), "bot sample message 1");
    ms = ms.with(botMessage1);
    WAMessage botMessage2 = ms.newMessageFromBot(start.plusSeconds(8), "bot sample message 2");
    ms = ms.with(botMessage2);
    assertThat(ms.messages()).hasSize(4);
    assertThat(ms.messages().get(0).instanceId()).isSameAs(userMessage1.instanceId());
    assertThat(ms.messages().get(1).instanceId()).isSameAs(botMessage1.instanceId());
    assertThat(ms.messages().get(2).instanceId()).isSameAs(userMessage2.instanceId());
    assertThat(ms.messages().get(3).instanceId()).isSameAs(botMessage2.instanceId());
  }
}
