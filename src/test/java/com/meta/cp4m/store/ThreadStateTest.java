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
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ThreadStateTest {

  private static final MessageFactory<FBMessage> FACTORY = MessageFactory.instance(FBMessage.class);

  static Stream<MessageFactory<?>> factories() {
    return MessageFactory.FACTORY_MAP.values().stream();
  }

  @ParameterizedTest
  @MethodSource("factories")
  void userData(MessageFactory<?> factory) {
    Instant start = Instant.now();
    Message message1 =
        factory.newMessage(
            start,
            new Payload.Text("sample message"),
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
    ThreadState<?> ts = ThreadState.of(message1);
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
    ThreadState<?> tsWithUserData = ts.withUserData(userdata);
    assertThat(tsWithUserData).isNotSameAs(ts);
    assertThat(tsWithUserData.userData()).isEqualTo(userdata);
  }

  @ParameterizedTest
  @MethodSource("factories")
  <T extends Message> void orderPreservation(MessageFactory<T> factory) {
    Instant start = Instant.now();
    T message1 =
        factory.newMessage(
            start,
            new Payload.Text("sample message"),
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);

    ThreadState<T> ms = ThreadState.of(message1);
    T message2 = ms.newMessageFromBot(start.plusSeconds(1), "other sample message");
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

  @ParameterizedTest
  @MethodSource("factories")
  <T extends Message> void orderCorrection(MessageFactory<T> factory) {
    Instant start = Instant.now();
    T message2 =
        factory.newMessage(
            start,
            new Payload.Text("sample message"),
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
    ThreadState<T> ms = ThreadState.of(message2);

    T message1 = ms.newMessageFromBot(start.minusSeconds(1), "other sample message");

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

  @ParameterizedTest
  @MethodSource("factories")
  <T extends Message> void botAndUserId(MessageFactory<T> factory) {
    Instant start = Instant.now();
    T message1 =
        factory.newMessage(
            start,
            new Payload.Text("sample message"),
            Identifier.random(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);

    ThreadState<T> ms = ThreadState.of(message1);
    T message2 =
        factory.newMessage(
            start,
            new Payload.Text("sample message"),
            message1.recipientId(),
            message1.senderId(),
            Identifier.random(),
            Message.Role.ASSISTANT);

    final ThreadState<T> finalMs = ms;
    assertThatCode(() -> finalMs.with(message2)).doesNotThrowAnyException();
    assertThatCode(() -> finalMs.with(finalMs.newMessageFromBot(start, "")))
        .doesNotThrowAnyException();
    assertThatCode(() -> finalMs.with(finalMs.newMessageFromUser(start, "", Identifier.random())))
        .doesNotThrowAnyException();
    ms = ms.with(message2);
    assertThat(ms.userId()).isEqualTo(message1.senderId());
    assertThat(ms.botId()).isEqualTo(message1.recipientId());
    T mDifferentSenderId =
        factory.newMessage(
            start,
            new Payload.Text(""),
            Identifier.random(),
            message1.recipientId(),
            Identifier.random(),
            Message.Role.USER);

    ThreadState<T> finalMs1 = ms;
    assertThatThrownBy(() -> finalMs1.with(mDifferentSenderId))
        .isInstanceOf(IllegalArgumentException.class);

    T mDifferentRecipientId =
        factory.newMessage(
            start,
            new Payload.Text(""),
            message1.senderId(),
            Identifier.random(),
            Identifier.random(),
            Message.Role.USER);
    assertThatThrownBy(() -> finalMs1.with(mDifferentRecipientId))
        .isInstanceOf(IllegalArgumentException.class);

    T illegalSenderId =
        factory.newMessage(
            start,
            new Payload.Text(""),
            message1.recipientId(),
            message1.senderId(),
            Identifier.random(),
            Message.Role.USER);
    assertThatThrownBy(() -> finalMs1.with(illegalSenderId))
        .isInstanceOf(IllegalArgumentException.class);

    T illegalRecipientId =
        factory.newMessage(
            start,
            new Payload.Text(""),
            message1.senderId(),
            message1.recipientId(),
            Identifier.random(),
            Message.Role.ASSISTANT);
    assertThatThrownBy(() -> finalMs1.with(illegalRecipientId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @MethodSource("factories")
  <T extends Message> void merge(MessageFactory<T> factory) {

    ThreadState<T> ts1 =
        ThreadState.of(
            factory.newMessage(
                Instant.now(),
                new Payload.Text("sample message 1"),
                Identifier.random(),
                Identifier.random(),
                Identifier.random(),
                Message.Role.USER));
    ThreadState<T> ts2 =
        ts1.with(ts1.newMessageFromBot(Instant.now(), "sample message 2"))
            .withUserData(ts1.userData().withPhoneNumber("+1 555 555 1234"));
    ts1 =
        ts1.with(ts1.newMessageFromBot(Instant.now(), "sample message 3"))
            .withUserData(ts1.userData().withName("name"));
    assertThat(ts1.messages()).hasSize(2);
    assertThat(ts2.messages()).hasSize(2);
    assertThat(ts1).isNotEqualTo(ts2);
    ThreadState<T> merged = ThreadState.merge(ts1, ts2);
    assertThat(merged)
        .isEqualTo(ThreadState.merge(ts2, ts1))
        .isEqualTo(ts1.merge(ts2))
        .isEqualTo(ts2.merge(ts1));
    assertThat(merged.messages()).hasSize(3);
    assertThat(merged.tail()).isSameAs(ts1.messages().getLast());
    assertThat(merged.messages().getFirst()).isSameAs(ts1.messages().getFirst());
    assertThat(merged.userData().phoneNumber()).get().isEqualTo("+1 555 555 1234");
    assertThat(merged.userData().name()).get().isEqualTo("name");
  }
}
