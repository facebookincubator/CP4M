/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message.webhook.whatsapp;

import com.google.common.base.Preconditions;
import java.util.Objects;

/**
 * @param field Notification type. Value will be "messages".
 * @param value @see {@link Value}
 */
public record Change(String field, Value value) {

  public Change {
    Objects.requireNonNull(value);
    Preconditions.checkArgument(Objects.equals(field, "messages"), "field must be set to messages");
  }
}
