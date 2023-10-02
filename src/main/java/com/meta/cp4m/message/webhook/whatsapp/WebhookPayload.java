/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message.webhook.whatsapp;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Objects;

/**
 * @param object The specific webhook a business is subscribed to. The webhook is
 *     whatsapp_business_account.
 * @param entry An array of entry objects
 */
public record WebhookPayload(String object, Collection<Entry> entry) {
  public WebhookPayload {
    Preconditions.checkArgument(Objects.equals(object, "whatsapp_business_account"));
    Preconditions.checkArgument(!Objects.requireNonNull(entry).isEmpty());
  }
}
