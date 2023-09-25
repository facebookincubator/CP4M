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
  /**
   * <a
   * href="https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/components#messages-object">Source
   * Documentation</a>
   *
   * @return When a customer clicks an ad that redirects to WhatsApp, this object is included in the
   *     messages object
   */
  Optional<Referral> referral();
}
