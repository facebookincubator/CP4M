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

  static Identifier threadId(Identifier id1, Identifier id2) {
    if (id1.compareTo(id2) <= 0) {
      return Identifier.from(id1.toString() + '|' + id2);
    }
    return Identifier.from(id2.toString() + '|' + id1);
  }

  default Identifier threadId() {
    return threadId(senderId(), recipientId());
  }
}
