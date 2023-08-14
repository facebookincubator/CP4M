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
import java.util.List;
import java.util.Objects;

public class MessageStack<T extends Message> {

  private final Runnable onClose;
  private final List<T> messages;

  MessageStack(Runnable onClose, List<T> messages) {
    this.onClose = Objects.requireNonNull(onClose);
    this.messages = ImmutableList.copyOf(Objects.requireNonNull(messages));
  }

  /** Constructor that exists to support the with method */
  private MessageStack(Runnable onClose, List<T> messages, T newMessage) {
    this.onClose = onClose;
    this.messages = ImmutableList.<T>builder().addAll(messages).add(newMessage).build();
  }

  public List<T> messages() {
    return messages;
  }

  public MessageStack<T> with(T message) {
    return new MessageStack<>(onClose, messages, message);
  }
}
