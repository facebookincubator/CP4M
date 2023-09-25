/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message.webhook.whatsapp;

import java.util.Optional;

public interface ReferableWebhookMessage extends WebhookMessage {
  Optional<Referral> referral();
}
