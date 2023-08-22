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

  record ConversationId(FBID recipientId, FBID senderId) {}

  Instant timestamp();

  String instanceId();

  FBID senderId();

  FBID recipientId();

  String message();

  Role role();

  enum Role {
    ASSISTANT,
    USER,
    SYSTEM
  }

  default ConversationId conversationId() {
    return new ConversationId(recipientId(), senderId());
  }
}
