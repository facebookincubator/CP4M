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
import com.meta.cp4m.message.webhook.whatsapp.WebhookMessage.ReferredProduct;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class WebhookMessageContext {
  private final boolean forwarded;

  @JsonProperty("frequently_forwarded")
  private final boolean frequentlyForwarded;

  private final Identifier from;
  private final Identifier id;

  private final @Nullable ReferredProduct referredProduct;

  @JsonCreator
  public WebhookMessageContext(
      @JsonProperty("forwarded") @Nullable Boolean forwarded,
      @JsonProperty("frequently_forwarded") @Nullable Boolean frequentlyForwarded,
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("referred_product") @Nullable ReferredProduct referredProduct) {
    this.forwarded = Objects.requireNonNullElse(forwarded, false);
    this.frequentlyForwarded = Objects.requireNonNullElse(frequentlyForwarded, false);
    this.from = Identifier.from(Objects.requireNonNull(from));
    this.id = Identifier.from(Objects.requireNonNull(id));
    this.referredProduct = referredProduct;
  }

  Optional<ReferredProduct> referredProduct() {
    return Optional.ofNullable(referredProduct);
  }

  public boolean forwarded() {
    return forwarded;
  }

  public boolean frequentlyForwarded() {
    return frequentlyForwarded;
  }

  public Identifier from() {
    return from;
  }

  public Identifier id() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (WebhookMessageContext) obj;
    return this.forwarded == that.forwarded
        && this.frequentlyForwarded == that.frequentlyForwarded
        && Objects.equals(this.from, that.from)
        && Objects.equals(this.id, that.id)
        && Objects.equals(this.referredProduct, that.referredProduct);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forwarded, frequentlyForwarded, from, id, referredProduct);
  }

  @Override
  public String toString() {
    return "WebhookMessageContext["
        + "forwarded="
        + forwarded
        + ", "
        + "frequentlyForwarded="
        + frequentlyForwarded
        + ", "
        + "from="
        + from
        + ", "
        + "businessAccountId="
        + id
        + ", "
        + "referredProduct="
        + referredProduct
        + ']';
  }
}
