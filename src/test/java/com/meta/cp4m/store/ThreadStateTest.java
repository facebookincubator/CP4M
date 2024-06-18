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

  private static final MessageFactory<FBMessage> FACTORY = MessageFactory.instance(FBMessage.class);

  @Test
  void userData() {
    Instant start = Instant.now();
    FBMessage message1 =
        FACTORY.newMessage(
            start,
            new Payload.Text("sample message"),
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
    ThreadState<FBMessage> ts = ThreadState.of(message1);
    assertThat(ts.userData())
        .isNotNull()
        .satisfies(userData -> assertThat(userData.name()).isEmpty())
        .satisfies(userData -> assertThat(userData.phoneNumber()).isEmpty());

    assertThat(ts.userData().withName("Fuzzy Bunny"))
        .satisfies(ud -> assertThat(ud.name().orElseThrow()).isEqualTo("Fuzzy Bunny"));
    assertThat(ts.userData().withPhoneNumber("+1 555 555 1234"))
        .satisfies(ud -> assertThat(ud.phoneNumber().orElseThrow()).isEqualTo("+1 555 555 1234"));

    // cannot add blank numbers
    assertThatThrownBy(() -> ts.userData().withName(" "));
    assertThatThrownBy(() -> ts.userData().withName(null));
    assertThatThrownBy(() -> ts.userData().withPhoneNumber(null));

    UserData userdata = ts.userData().withName("FizzBuzz").withPhoneNumber("+1 (555) 555 4321");
    ThreadState<FBMessage> tsWithUserData = ts.withUserData(userdata);
    assertThat(tsWithUserData).isNotSameAs(ts);
    assertThat(tsWithUserData.userData()).isEqualTo(userdata);
  }

  @Test
  void orderPreservation() {
    Instant start = Instant.now();
    FBMessage message1 =
        FACTORY.newMessage(
            start,
            new Payload.Text("sample message"),
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
        FACTORY.newMessage(
            start,
            new Payload.Text("sample message"),
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
        FACTORY.newMessage(
            start,
            new Payload.Text("sample message"),
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);

    ThreadState<FBMessage> ms = ThreadState.of(message1);
    FBMessage message2 =
        FACTORY.newMessage(
            start,
            new Payload.Text("sample message"),
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
        FACTORY.newMessage(
            start,
            new Payload.Text(""),
            Identifier.random(),
            message1.recipientId(),
            Identifier.random(),
            Message.Role.USER);

    ThreadState<FBMessage> finalMs1 = ms;
    assertThatThrownBy(() -> finalMs1.with(mDifferentSenderId))
        .isInstanceOf(IllegalArgumentException.class);

    FBMessage mDifferentRecipientId =
        FACTORY.newMessage(
            start,
            new Payload.Text(""),
            message1.senderId(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
    assertThatThrownBy(() -> finalMs1.with(mDifferentRecipientId))
        .isInstanceOf(IllegalArgumentException.class);

    FBMessage illegalSenderId =
        FACTORY.newMessage(
            start,
            new Payload.Text(""),
            message1.recipientId(),
            message1.senderId(),
            Identifier.random(),
            Message.Role.USER);
    assertThatThrownBy(() -> finalMs1.with(illegalSenderId))
        .isInstanceOf(IllegalArgumentException.class);

    FBMessage illegalRecipientId =
        FACTORY.newMessage(
            start,
            new Payload.Text(""),
            message1.senderId(),
            message1.recipientId(),
            Identifier.random(),
            Message.Role.ASSISTANT);
    assertThatThrownBy(() -> finalMs1.with(illegalRecipientId))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
