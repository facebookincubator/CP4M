/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.Message.Role;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.common.reflection.qual.NewInstance;

public class ThreadState<T extends Message> {
  private final List<T> messages;
  private final MessageFactory<T> messageFactory;

  private final UserData userData;

  private ThreadState(T message) {
    Objects.requireNonNull(message);
    this.messages = ImmutableList.of(message);
    messageFactory = MessageFactory.instance(message);
    userData = UserData.create(this.userId());
  }

  public ThreadState(List<T> messages, MessageFactory<T> messageFactory, UserData userData) {
    this.messages = messages;
    this.messageFactory = messageFactory;
    this.userData = userData;
  }

  private ThreadState(ThreadState<T> old, UserData userData) {
    this.messages = old.messages;
    this.messageFactory = old.messageFactory;
    this.userData = userData;
  }

  /** Constructor that exists to support the with method */
  private ThreadState(ThreadState<T> old, T newMessage) {
    Objects.requireNonNull(newMessage);
    userData = old.userData;
    messageFactory = old.messageFactory;
    Preconditions.checkArgument(
        old.tail().threadId().equals(newMessage.threadId()),
        "all messages in a thread must have the same thread id");
    List<T> messages = old.messages;
    if (newMessage.timestamp().isAfter(old.tail().timestamp())) {
      this.messages = ImmutableList.<T>builder().addAll(messages).add(newMessage).build();
    } else {
      this.messages =
          Stream.concat(messages.stream(), Stream.of(newMessage))
              .sorted(Comparator.comparing(Message::timestamp))
              .distinct()
              .collect(Collectors.toUnmodifiableList());
    }

    Preconditions.checkArgument(
        old.userId().equals(userId()) && old.botId().equals(botId()),
        "userId and botId not consistent with this thread state");
  }

  public static <T extends Message> ThreadState<T> of(T message) {
    return new ThreadState<>(message);
  }

  public Identifier userId() {
    T message = tail();
    return switch (message.role()) {
      case ASSISTANT -> message.recipientId();
      case USER -> message.senderId();
    };
  }

  public Identifier botId() {
    T message = tail();
    return switch (message.role()) {
      case ASSISTANT -> message.senderId();
      case USER -> message.recipientId();
    };
  }

  public T newMessageFromBot(Instant timestamp, String message) {
    return messageFactory.newMessage(
        timestamp,
        new Payload.Text(message),
        botId(),
        userId(),
        Identifier.random(),
        Role.ASSISTANT);
  }

  public T newMessageFromBot(Instant timestamp, Payload<?> payload) {
    return messageFactory.newMessage(
        timestamp, payload, botId(), userId(), Identifier.random(), Role.ASSISTANT);
  }

  public T newMessageFromUser(Instant timestamp, String message, Identifier instanceId) {
    return messageFactory.newMessage(
        timestamp, new Payload.Text(message), userId(), botId(), instanceId, Role.USER);
  }

  public ThreadState<T> with(T message) {
    return new ThreadState<>(this, message);
  }

  public List<T> messages() {
    return messages;
  }

  public T tail() {
    return messages.getLast();
  }

  public UserData userData() {
    return userData;
  }

  public @NewInstance ThreadState<T> withUserData(UserData userData) {
    return new ThreadState<>(this, userData);
  }

  public static <T extends Message> ThreadState<T> merge(ThreadState<T> t1, ThreadState<T> t2) {
    Preconditions.checkArgument(t1.userId().equals(t2.userId()));
    Preconditions.checkArgument(t1.botId().equals(t2.botId()));
    List<T> messages =
        Stream.concat(t1.messages.stream(), t2.messages.stream())
            .sorted(Comparator.comparing(Message::timestamp))
            .distinct()
            .collect(Collectors.toUnmodifiableList());
    return new ThreadState<>(messages, t1.messageFactory, UserData.merge(t1.userData, t2.userData));
  }

  public @NewInstance ThreadState<T> merge(ThreadState<T> other) {
    return merge(this, other);
  }

  public Identifier threadId() {
    return tail().threadId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ThreadState<?> that = (ThreadState<?>) o;
    return Objects.equals(messages, that.messages) && Objects.equals(userData, that.userData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(messages, userData);
  }
}
