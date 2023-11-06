/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.meta.cp4m.Identifier;
import java.time.Instant;

public interface Message {

  static Identifier threadId(Identifier id1, Identifier id2) {
    if (id1.compareTo(id2) <= 0) {
      return Identifier.from(id1.toString() + '|' + id2);
    }
    return Identifier.from(id2.toString() + '|' + id1);
  }

  Instant timestamp();

  Identifier instanceId();

  Identifier senderId();

  Identifier recipientId();

  String message();

  Role role();

  default Identifier threadId() {
    return threadId(senderId(), recipientId());
  }

  enum Role {
    ASSISTANT,
    USER,
    SYSTEM
  }
}
