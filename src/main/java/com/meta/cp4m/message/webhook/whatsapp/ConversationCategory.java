/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message.webhook.whatsapp;

public enum ConversationCategory {
  AUTHENTICATION,
  MARKETING,
  UTILITY,
  SERVICE,
  REFERRAL_CONVERSION;

  public String toString() {
    return this.name().toLowerCase();
  }
}
