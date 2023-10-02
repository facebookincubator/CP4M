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
import com.meta.cp4m.Identifier;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SystemWebhookMessage implements WebhookMessage {
  private final System system;
  private final @Nullable WebhookMessageContext context;
  private final Collection<Error> errors;
  private final Identifier from;
  private final Identifier id;
  private final Instant timestamp;

  @JsonCreator
  public SystemWebhookMessage(
      @JsonProperty("system") System system,
      @JsonProperty("context") @Nullable WebhookMessageContext context,
      @JsonProperty("error") Collection<Error> errors,
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") long timestamp) {
    this.system = Objects.requireNonNull(system);
    this.context = context;
    this.errors = errors == null ? Collections.emptyList() : errors;
    this.from = Identifier.from(Objects.requireNonNull(from));
    this.id = Identifier.from(Objects.requireNonNull(id));
    this.timestamp = Instant.ofEpochSecond(timestamp);
  }

  public System system() {
    return system;
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
    return WebhookMessageType.SYSTEM;
  }

  public enum SystemType {
    CUSTOMER_CHANGED_NUMBER,
    CUSTOMER_IDENTITY_CHANGED;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  public record System(
      String body, String identity, Identifier waId, SystemType type, Identifier customer) {

    @JsonCreator
    public System(
        @JsonProperty("body") String body,
        @JsonProperty("identity") String identity,
        @JsonProperty("wa_id") String waId,
        @JsonProperty("type") SystemType type,
        @JsonProperty("customer") String customer) {
      this(
          Objects.requireNonNull(body),
          Objects.requireNonNull(identity),
          Identifier.from(Objects.requireNonNull(waId)),
          Objects.requireNonNull(type),
          Identifier.from(Objects.requireNonNull(customer)));
    }
  }
}
