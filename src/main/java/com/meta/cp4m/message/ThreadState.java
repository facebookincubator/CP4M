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

public class ThreadState<T extends Message> {
  private final List<T> messages;
  private final MessageFactory<T> messageFactory;

  private ThreadState(T message) {
    Objects.requireNonNull(message);
    Preconditions.checkArgument(
        message.role() != Role.SYSTEM, "ThreadState should never hold a system message");
    this.messages = ImmutableList.of(message);
    messageFactory = MessageFactory.instance(message);
  }

  /** Constructor that exists to support the with method */
  private ThreadState(ThreadState<T> old, T newMessage) {
    Objects.requireNonNull(newMessage);
    Preconditions.checkArgument(
            newMessage.role() != Role.SYSTEM, "ThreadState should never hold a system message");
    messageFactory = old.messageFactory;
    Preconditions.checkArgument(
        old.tail().threadId().equals(newMessage.threadId()),
        "all messages in a thread must have the same thread id");
    List<T> messages = old.messages;
    T mWithParentMessage = newMessage.role() == Role.USER ? newMessage.withParentMessage(old.tail()): newMessage;
    this.messages =
          Stream.concat(messages.stream(), Stream.of(mWithParentMessage))
              .sorted((m1,m2) -> m1.parentMessage() == m2.parentMessage() ? compare(m1.role().getPriority(),m2.role().getPriority()) : (m1.timestamp().compareTo(m2.timestamp())))
              .collect(Collectors.toUnmodifiableList());
    Identifier oldUserId = old.userId();
    Identifier userId = userId();
    Identifier oldBotId = old.botId();
    Identifier botId = botId();
    Preconditions.checkArgument(
        old.userId().equals(userId()) && old.botId().equals(botId()),
        "userId and botId not consistent with this thread state");
  }

  private int compare(int priority1, int priority2){
    return priority1 > priority2 ? +1 : priority1 < priority2 ? -1 : 0;
  }

  public static <T extends Message> ThreadState<T> of(T message) {
    return new ThreadState<>(message);
  }

  public Identifier userId() {
    T message = tail();
    return switch (message.role()) {
      case ASSISTANT -> message.recipientId();
      case USER -> message.senderId();
      case SYSTEM -> throw new IllegalStateException(
          "ThreadState should never hold a SYSTEM message");
    };
  }

  public Identifier botId() {
    T message = tail();
    return switch (message.role()) {
      case ASSISTANT -> message.senderId();
      case USER -> message.recipientId();
      case SYSTEM -> throw new IllegalStateException(
          "ThreadState should never hold a SYSTEM message");
    };
  }

  public T newMessageFromBot(Instant timestamp, String message) {
    T newMessage = messageFactory.newMessage(
        timestamp, message, botId(), userId(), Identifier.random(), Role.ASSISTANT);
    return newMessage.withParentMessage(tail());
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
