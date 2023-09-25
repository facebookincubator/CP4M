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

public class ImageWebhookMessage implements ReferableWebhookMessage {
  private final @Nullable Referral referral;
  private final Image image;
  private final @Nullable WebhookMessageContext context;
  private final Collection<Error> errors;
  private final Identifier from;
  private final Identifier id;
  private final Instant timestamp;

  @JsonCreator
  public ImageWebhookMessage(
      @JsonProperty("image") Image image,
      @JsonProperty("context") @Nullable WebhookMessageContext context,
      @JsonProperty("error") Collection<Error> errors,
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") long timestamp,
      @JsonProperty("referral") @Nullable Referral referral) {
    this.referral = referral;
    this.image = Objects.requireNonNull(image);
    this.context = context;
    this.errors = errors == null ? Collections.emptyList() : errors;
    this.from = Identifier.from(Objects.requireNonNull(from));
    this.id = Identifier.from(Objects.requireNonNull(id));
    this.timestamp = Instant.ofEpochSecond(timestamp);
  }

  @Override
  public Optional<Referral> referral() {
    return Optional.ofNullable(referral);
  }

  public Image image() {
    return image;
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
    return WebhookMessageType.IMAGE;
  }

  public record Image(
      String caption, String sha256, String id, @JsonProperty("mime_type") String mimeType) {}
}
