/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message.webhook.whatsapp;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Objects;

public record WebhookPayload(String object, Collection<Entry> entry) {
  public WebhookPayload {
    Preconditions.checkArgument(Objects.equals(object, "whatsapp_business_account"));
    Preconditions.checkArgument(!Objects.requireNonNull(entry).isEmpty());
  }
}
