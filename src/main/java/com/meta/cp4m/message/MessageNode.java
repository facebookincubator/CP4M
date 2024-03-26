/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import org.checkerframework.checker.nullness.qual.Nullable;

public class MessageNode<T extends Message> {
  T message;
  @Nullable MessageNode<T> parentMessage;

  public MessageNode(T message) {
    this.message = message;
    this.parentMessage = null;
  }

  public MessageNode(T message, @Nullable MessageNode<T> parentMessage) {
    this.message = message;
    this.parentMessage = parentMessage;
  }

  public T message() {
    return message;
  }

  public @Nullable MessageNode<T> parentMessage() {
    return parentMessage;
  }
}
