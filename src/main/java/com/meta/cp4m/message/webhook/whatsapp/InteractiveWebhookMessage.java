/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message.webhook.whatsapp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meta.cp4m.Identifier;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InteractiveWebhookMessage implements WebhookMessage {
  private final ObjectNode interactive;
  private final @Nullable WebhookMessageContext context;
  private final Collection<Error> errors;
  private final Identifier from;
  private final Identifier id;
  private final Instant timestamp;

  @JsonCreator
  public InteractiveWebhookMessage(
      @JsonProperty("interactive") ObjectNode interactive,
      @JsonProperty("context") @Nullable WebhookMessageContext context,
      @JsonProperty("error") Collection<Error> errors,
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") long timestamp) {
    this.interactive = Objects.requireNonNull(interactive);
    this.context = context;
    this.errors = errors == null ? Collections.emptyList() : errors;
    this.from = Identifier.from(Objects.requireNonNull(from));
    this.id = Identifier.from(Objects.requireNonNull(id));
    this.timestamp = Instant.ofEpochSecond(timestamp);
  }

  public ObjectNode interactive() {
    return interactive;
  }

  @Override
  public Optional<WebhookMessageContext> context() {
    return Optional.ofNullable(context);
  }

  @Override
  public Collection<Error> errors() {
    return errors;
  }

  @Override
  public Identifier from() {
    return from;
  }

  @Override
  public Identifier id() {
    return id;
  }

  @Override
  public Instant timestamp() {
    return timestamp;
  }

  @Override
  public WebhookMessageType type() {
    return WebhookMessageType.INTERACTIVE;
  }
}
