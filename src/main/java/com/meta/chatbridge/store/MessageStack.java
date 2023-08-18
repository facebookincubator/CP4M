/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.store;

import com.google.common.collect.ImmutableList;
import com.meta.chatbridge.message.Message;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageStack<T extends Message> {

  private final List<T> messages;

  private MessageStack(Collection<T> messages) {
    Objects.requireNonNull(messages);
    this.messages =
        messages.stream()
            .sorted(Comparator.comparing(Message::timestamp))
            .collect(Collectors.toUnmodifiableList());
  }

  /** Constructor that exists to support the with method */
  private MessageStack(List<T> messages, T newMessage) {
    if (!messages.isEmpty()
        && newMessage.timestamp().isBefore(messages.get(messages.size() - 1).timestamp())) {
      this.messages =
          Stream.concat(messages.stream(), Stream.of(newMessage))
              .sorted(Comparator.comparing(Message::timestamp))
              .collect(Collectors.toUnmodifiableList());
    } else {
      this.messages = ImmutableList.<T>builder().addAll(messages).add(newMessage).build();
    }
  }

  public static <T extends Message> MessageStack<T> of(T message) {
    return new MessageStack<>(List.of(message));
  }

  public static <T extends Message> MessageStack<T> of(Collection<T> messages) {
    return new MessageStack<>(messages);
  }

  public MessageStack<T> with(T message) {
    return new MessageStack<>(messages, message);
  }

  public List<T> messages() {
    return messages;
  }
}
