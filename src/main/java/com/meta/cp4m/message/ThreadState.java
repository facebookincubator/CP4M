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
  private final List<MessageNode<T>> messageNodes;
  private final MessageFactory<T> messageFactory;

  private ThreadState(T message) {
    Objects.requireNonNull(message);
    Preconditions.checkArgument(
        message.role() != Role.SYSTEM, "ThreadState should never hold a system message");
    MessageNode<T> messageNode = new MessageNode<>(message, null);
    this.messageNodes = ImmutableList.of(messageNode);
    messageFactory = MessageFactory.instance(message);
  }

  private ThreadState(List<MessageNode<T>> nodes, MessageFactory<T> factory) {
    this.messageNodes = nodes;
    this.messageFactory = factory;
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
    //    MessageNode<T> mWithParentMessage = new MessageNode<>(newMessage, old.tail());
    MessageNode<T> mWithParentMessage = new MessageNode<>(newMessage);
    this.messageNodes =
        Stream.concat(messageNodes.stream(), Stream.of(mWithParentMessage))
            .sorted(
                (m1, m2) ->
                    m1.parentMessage() == m2.parentMessage()
                        ? compare(m1.message().role().priority(), m2.message().role().priority())
                        : (m1.message().timestamp().compareTo(m2.message().timestamp())))
            .collect(Collectors.toUnmodifiableList());
    Preconditions.checkArgument(
        old.userId().equals(userId()) && old.botId().equals(botId()),
        "userId and botId not consistent with this thread state");
  }

  public static <T extends Message> ThreadState<T> merge(
      ThreadState<T> first, ThreadState<T> second) {
    List<MessageNode<T>> firstMessages = first.messageNodes;
    List<MessageNode<T>> secondMessages = second.messageNodes;
    Preconditions.checkState(
        firstMessages.get(0).equals(secondMessages.get(0)),
        "attempting to merge disconnected instances of " + ThreadState.class.getCanonicalName());

    List<MessageNode<T>> result =
        new ArrayList<>(Math.max(firstMessages.size(), secondMessages.size()));

    int firstLocation = 0;
    int secondLocation = 0;

    while (firstLocation < firstMessages.size() && secondLocation < secondMessages.size()) {
      //      if (firstLocation >= firstMessages.size() && secondLocation <)
      MessageNode<T> firstNode = firstMessages.get(firstLocation);
      MessageNode<T> secondNode = secondMessages.get(secondLocation);
      if (firstNode.message.threadId().equals(secondNode.message.threadId())) {
        if (firstNode.parentMessage() == null) {
          Preconditions.checkState(secondNode.parentMessage() == null);
          result.add(new MessageNode<>(firstNode.message()));
        } else {
          Preconditions.checkState(
              Objects.equals(firstNode.parentMessage(), secondNode.parentMessage()));
          Identifier threadId =
              Objects.requireNonNull(firstNode.parentMessage()).message().threadId();

          // search backward through the array for the parent message node
          // the parent will almost always be in one of the last two positions of the array
          for (int i = result.size() - 1; i >= 0; i--) {
            MessageNode<T> mn = result.get(i);
            if (mn.message().threadId().equals(threadId)) {
              result.add(new MessageNode<>(firstNode.message(), mn));
            }
          }
        }
        firstLocation += 1;
        secondLocation += 1;
      }
    }
    return new ThreadState<>(result, first.messageFactory);
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

  private int compare(int priority1, int priority2) {
    return Integer.compare(priority1, priority2);
  }

  public T newMessageFromUser(Instant timestamp, String message, Identifier instanceId) {
    return messageFactory.newMessage(timestamp, message, userId(), botId(), instanceId, Role.USER);
  }

  public @NewInstance ThreadState<T> withNewMessageFromBot(Instant timestamp, String message) {
    T newMessage =
        messageFactory.newMessage(
            timestamp, message, botId(), userId(), Identifier.random(), Role.ASSISTANT);
    return with(newMessage);
  }

  public ThreadState<T> withNewMessageFromUser(
      Instant timestamp, String message, Identifier instanceId) {
    T newMessage =
        messageFactory.newMessage(timestamp, message, userId(), botId(), instanceId, Role.USER);
    return with(newMessage);
  }

  public ThreadState<T> with(T message) {
    return new ThreadState<>(this, this, message);
  }

  public List<T> messages() {
    return messageNodes.stream().map(MessageNode::message).collect(Collectors.toList());
  }

  public T tail() {
    return messageNodes.get(messageNodes.size() - 1).message();
  }

  public Identifier threadId() {
    return tail().threadId();
  }
}
