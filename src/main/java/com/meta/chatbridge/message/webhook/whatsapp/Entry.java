/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message.webhook.whatsapp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meta.chatbridge.Identifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * @param businessAccountId The WhatsApp Business Account ID for the business that is subscribed to
 *     the webhook
 * @param changes An array of change objects
 */
public record Entry(Identifier businessAccountId, Collection<Change> changes) {

  @JsonCreator
  public Entry(@JsonProperty("id") String id, @JsonProperty("changes") Collection<Change> changes) {
    this(
        Identifier.from(Objects.requireNonNull(id)),
        changes == null ? Collections.emptyList() : changes);
  }
}
