/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message.webhook.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.meta.cp4m.Identifier;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * The messages array of objects is nested within the value object and is triggered when a customer
 * updates their profile information or a customer sends a message to the business that is
 * subscribed to the webhook.
 */
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

  /**
   * <a
   * href="https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/components#messages-object">Source
   * Documentation</a>
   *
   * @return The customer's WhatsApp ID. A business can respond to a customer using this ID. This ID
   *     may not match the customer's phone number, which is returned by the API as input when
   *     sending a message to the customer.
   */
  Collection<Error> errors();

  /**
   * <a
   * href="https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/components#messages-object">Source
   * Documentation</a>
   *
   * @return The customer's WhatsApp ID. A business can respond to a customer using this ID. This ID
   *     may not match the customer's phone number, which is returned by the API as input when
   *     sending a message to the customer.
   */
  Identifier from();

  /**
   * <a
   * href="https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/components#messages-object">Source
   * Documentation</a>
   *
   * @return The ID for the message that was received by the business. You could use messages
   *     endpoint to mark this specific message as read.
   */
  Identifier id();

  /**
   * <a
   * href="https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/components#messages-object">Source
   * Documentation</a>
   *
   * @return timestamp indicating when the WhatsApp server received the message from the customer.
   */
  Instant timestamp();

  /**
   * <a
   * href="https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/components#messages-object">Source
   * Documentation</a>
   *
   * @return The type of message that has been received by the business that has subscribed to
   *     Webhooks
   */
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
