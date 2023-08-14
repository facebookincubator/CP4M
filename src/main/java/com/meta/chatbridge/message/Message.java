/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.meta.chatbridge.FBID;
import java.time.Instant;

public interface Message {
  Instant timestamp();

  FBID conversationId();

  FBID senderId();

  FBID recipientId();

  String message();

  Role role();

  byte[] serialize();

  enum Role {
    ASSISTANT,
    USER,
    SYSTEM
  }
}
