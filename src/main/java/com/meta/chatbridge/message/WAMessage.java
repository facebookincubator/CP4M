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

public record WAMessage(
    Instant timestamp,
    Identifier instanceId,
    Identifier senderId,
    Identifier recipientId,
    String message,
    Role role)
    implements Message {}
