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
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ButtonWebhookMessage implements WebhookMessage {
  private final Button button;
  private final @Nullable WebhookMessageContext context;
  private final Collection<Error> errors;
  private final Identifier from;
  private final Identifier id;
  private final Instant timestamp;
  @JsonCreator
  public ButtonWebhookMessage(
      @JsonProperty("button") Button button,
      @JsonProperty("context") @Nullable WebhookMessageContext context,
      @JsonProperty("error") Collection<Error> errors,
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") long timestamp) {
    this.button = Objects.requireNonNull(button);
    this.context = context;
    this.errors = errors == null ? Collections.emptyList() : errors;
    this.from = Identifier.from(Objects.requireNonNull(from));
    this.id = Identifier.from(Objects.requireNonNull(id));
    this.timestamp = Instant.ofEpochSecond(timestamp);
  }

  public Button button() {
    return button;
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
    return WebhookMessageType.BUTTON;
  }

  public record Button(String payload, String text) {}
}
