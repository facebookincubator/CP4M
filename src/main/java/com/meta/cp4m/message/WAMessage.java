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

public record WAMessage(
    Instant timestamp,
    Identifier instanceId,
    Identifier senderId,
    Identifier recipientId,
    String message,
    Role role)
    implements Message {}
