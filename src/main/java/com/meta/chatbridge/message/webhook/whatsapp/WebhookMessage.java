/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message.webhook.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.meta.chatbridge.Identifier;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AudioWebhookMessage.class, name = "audio"),
  @JsonSubTypes.Type(value = ButtonWebhookMessage.class, name = "button"),
  @JsonSubTypes.Type(value = DocumentWebhookMessage.class, name = "document"),
  @JsonSubTypes.Type(value = TextWebhookMessage.class, name = "text"),
  @JsonSubTypes.Type(value = ImageWebhookMessage.class, name = "image"),
  @JsonSubTypes.Type(value = InteractiveWebhookMessage.class, name = "interactive"),
  @JsonSubTypes.Type(value = OrderWebhookMessage.class, name = "order"),
  @JsonSubTypes.Type(value = StickerWebhookMessage.class, name = "sticker"),
  @JsonSubTypes.Type(value = UnknownWebhookMessage.class, name = "unknown"),
  @JsonSubTypes.Type(value = VideoWebhookMessage.class, name = "video"),
})
public interface WebhookMessage {

  Optional<WebhookMessageContext> context();

  Collection<Error> errors();

  Identifier from();

  Identifier id();

  Instant timestamp();

  WebhookMessageType type();

  enum WebhookMessageType {
    AUDIO,
    BUTTON,
    DOCUMENT,
    TEXT,
    IMAGE,
    INTERACTIVE,
    ORDER,
    STICKER,
    SYSTEM,
    VIDEO,
    UNKNOWN;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  record ReferredProduct(
      @JsonProperty("catalog_id") String catalogId,
      @JsonProperty("product_retailer_id") String productRetailerId) {}
}
