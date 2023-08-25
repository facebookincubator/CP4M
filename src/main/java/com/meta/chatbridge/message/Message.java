/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.meta.chatbridge.Identifier;
import java.time.Instant;

public interface Message {


  Instant timestamp();

  Identifier instanceId();

  Identifier senderId();

  Identifier recipientId();

  String message();

  Role role();

  enum Role {
    ASSISTANT,
    USER,
    SYSTEM
  }

  default Identifier conversationId() {
    if (senderId().compare(senderId(), recipientId()) >= 0) {
      return Identifier.from(senderId().toString() + recipientId());
    }
    return Identifier.from(recipientId().toString() + senderId());
  }
}
