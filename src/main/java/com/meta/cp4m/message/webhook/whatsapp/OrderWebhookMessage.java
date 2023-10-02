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

public class OrderWebhookMessage implements WebhookMessage {
  private final Order order;
  private final @Nullable WebhookMessageContext context;
  private final Collection<Error> errors;
  private final Identifier from;
  private final Identifier id;
  private final Instant timestamp;
  @JsonCreator
  public OrderWebhookMessage(
      @JsonProperty("order") Order order,
      @JsonProperty("context") @Nullable WebhookMessageContext context,
      @JsonProperty("error") Collection<Error> errors,
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") long timestamp) {
    this.order = Objects.requireNonNull(order);
    this.context = context;
    this.errors = errors == null ? Collections.emptyList() : errors;
    this.from = Identifier.from(Objects.requireNonNull(from));
    this.id = Identifier.from(Objects.requireNonNull(id));
    this.timestamp = Instant.ofEpochSecond(timestamp);
  }

  public Order order() {
    return order;
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
    return WebhookMessageType.ORDER;
  }

  public record ProductItem(
      @JsonProperty("product_retailer_id") String productRetailerId,
      long quantity,
      @JsonProperty("item_price") String itemPrice,
      String currency) {
    public ProductItem {
      Objects.requireNonNull(productRetailerId);
      Objects.requireNonNull(itemPrice);
      Objects.requireNonNull(currency);
    }
  }

  public record Order(String catalogId, String text, Collection<ProductItem> productItems) {

    @JsonCreator
    public Order(
        @JsonProperty("catalog_id") String catalogId,
        @JsonProperty("text") String text,
        @JsonProperty("product_items") Collection<ProductItem> productItems) {
      this.catalogId = Objects.requireNonNull(catalogId);
      this.text = Objects.requireNonNull(text);
      this.productItems = productItems == null ? Collections.emptyList() : productItems;
    }
  }
}
