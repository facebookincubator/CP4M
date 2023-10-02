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
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Status {
  private final Identifier id;
  private final Collection<Error> errors;
  private final Identifier recipientId;
  private final StatusType statusType;
  private final Instant timestamp;

  @JsonCreator
  public Status(
      @JsonProperty("id") String id,
      @JsonProperty("errors") @Nullable Collection<Error> errors,
      @JsonProperty("recipient_id") String recipientId,
      @JsonProperty("status") StatusType statusType,
      @JsonProperty("timestamp") long timestamp) {
    this.id = Identifier.from(Objects.requireNonNull(id));
    this.errors = errors == null ? Collections.emptyList() : errors;
    this.recipientId = Identifier.from(Objects.requireNonNull(recipientId));
    this.statusType = Objects.requireNonNull(statusType);
    this.timestamp = Instant.ofEpochSecond(timestamp);
  }

  public Identifier id() {
    return id;
  }

  public Collection<Error> errors() {
    return errors;
  }

  public Identifier recipientId() {
    return recipientId;
  }

  public StatusType status() {
    return statusType;
  }

  public Instant timestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (Status) obj;
    return Objects.equals(this.id, that.id)
        && Objects.equals(this.errors, that.errors)
        && Objects.equals(this.recipientId, that.recipientId)
        && Objects.equals(this.statusType, that.statusType)
        && Objects.equals(this.timestamp, that.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, errors, recipientId, statusType, timestamp);
  }

  public enum StatusType {
    DELIVERED,
    READ,
    SENT;

    public String toString() {
      return this.name().toLowerCase();
    }
  }

  public record Pricing(
      @JsonProperty("pricing_model") String pricingModel, ConversationCategory category) {
    public Pricing {
      Objects.requireNonNull(pricingModel);
      Objects.requireNonNull(category);
    }
  }
}
