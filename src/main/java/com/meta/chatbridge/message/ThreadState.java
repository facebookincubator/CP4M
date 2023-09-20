/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.meta.chatbridge.Identifier;
import com.meta.chatbridge.message.Message.Role;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThreadState<T extends Message> {
  private final List<T> messages;
  private final MessageFactory<T> messageFactory;

  private ThreadState(T message) {
    Objects.requireNonNull(message);
    this.messages = ImmutableList.of(message);
    messageFactory = MessageFactory.instance(message);
  }

  /** Constructor that exists to support the with method */
  private ThreadState(ThreadState<T> old, T newMessage) {
    Objects.requireNonNull(newMessage);
    messageFactory = old.messageFactory;
    Preconditions.checkArgument(
        old.tail().threadId().equals(newMessage.threadId()),
        "all messages in a thread must have the same thread id");
    List<T> messages = old.messages;
    if (newMessage.timestamp().isBefore(old.tail().timestamp())) {
      this.messages =
          Stream.concat(messages.stream(), Stream.of(newMessage))
              .sorted(Comparator.comparing(Message::timestamp))
              .collect(Collectors.toUnmodifiableList());
    } else {
      this.messages = ImmutableList.<T>builder().addAll(messages).add(newMessage).build();
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
      case ASSISTANT, SYSTEM -> message.recipientId();
      case USER -> message.senderId();
    };
  }

  public Identifier botId() {
    T message = tail();
    return switch (message.role()) {
      case ASSISTANT, SYSTEM -> message.senderId();
      case USER -> message.recipientId();
    };
  }

  public T newMessageFromBot(Instant timestamp, String message) {
    return messageFactory.newMessage(
        timestamp, message, botId(), userId(), Identifier.random(), Role.ASSISTANT);
  }

  public T newMessageFromUser(Instant timestamp, String message, Identifier instanceId) {
    return messageFactory.newMessage(timestamp, message, userId(), botId(), instanceId, Role.USER);
  }

  public ThreadState<T> with(T message) {
    return new ThreadState<>(this, message);
  }

  public List<T> messages() {
    return messages;
  }

  public T tail() {
    return messages.get(messages.size() - 1);
  }
}
