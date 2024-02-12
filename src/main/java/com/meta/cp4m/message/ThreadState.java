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
  private final List<MessageNode<T>> messageNodes;
  private final MessageFactory<T> messageFactory;

  private ThreadState(T message) {
    Objects.requireNonNull(message);
    Preconditions.checkArgument(
        message.role() != Role.SYSTEM, "ThreadState should never hold a system message");
    MessageNode<T> messageNode = new MessageNode<>(message,null);
    this.messageNodes = ImmutableList.of(messageNode);
    messageFactory = MessageFactory.instance(message);
  }

  /** Constructor that exists to support the with method */
  private ThreadState(ThreadState<T> current, ThreadState<T> old, T newMessage) {
    Objects.requireNonNull(newMessage);
    Preconditions.checkArgument(
            newMessage.role() != Role.SYSTEM, "ThreadState should never hold a system message");
    messageFactory = current.messageFactory;
    Preconditions.checkArgument(
            old.tail().threadId().equals(newMessage.threadId()),
            "all messages in a thread must have the same thread id");
    List<MessageNode<T>> messageNodes = current.messageNodes;
    MessageNode<T> mWithParentMessage = new MessageNode<>(newMessage,old.tail());
    this.messageNodes =
            Stream.concat(messageNodes.stream(), Stream.of(mWithParentMessage))
                    .sorted((m1,m2) -> m1.getParentMessage() == m2.getParentMessage() ? compare(m1.getMessage().role().priority(),m2.getMessage().role().priority()) : (m1.getMessage().timestamp().compareTo(m2.getMessage().timestamp())))
                    .collect(Collectors.toUnmodifiableList());

    Preconditions.checkArgument(
            old.userId().equals(userId()) && old.botId().equals(botId()),
            "userId and botId not consistent with this thread state");
  }

  private int compare(int priority1, int priority2){
    return Integer.compare(priority1, priority2);
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
    return messageFactory.newMessage(
        timestamp, message, botId(), userId(), Identifier.random(), Role.ASSISTANT);
  }

  public T newMessageFromUser(Instant timestamp, String message, Identifier instanceId) {
    return messageFactory.newMessage(timestamp, message, userId(), botId(), instanceId, Role.USER);
  }

  public ThreadState<T> with(T message) {
    return new ThreadState<>(this,this, message);
  }

  public ThreadState<T> with(ThreadState<T> thread,T message) {
    return new ThreadState<>(this,thread, message);
  }

  public List<T> messages() {
    return messageNodes.stream().map(MessageNode::getMessage).collect(Collectors.toList());
  }

  public T tail() {
    return messageNodes.get(messageNodes.size() - 1).getMessage();
  }

}
