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
import com.google.common.base.Preconditions;
import com.meta.chatbridge.Identifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The value object contains details for the change that triggered the webhook. This object is
 * nested within the changes array of the entry array.
 *
 * @param messagingProduct Product used to send the message. Value is always whatsapp.
 * @param metadata A metadata object describing the business subscribed to the webhook.
 * @param contacts Array of contact objects with information for the customer who sent a message to
 *     the business.
 * @param errors An array of error objects describing the error. Error objects have the following
 *     properties, which map to their equivalent properties in API error response payloads.
 * @param statuses Status object for a message that was sent by the business that is subscribed to
 *     the webhook
 * @param messages Information about a message received by the business that is subscribed to the
 *     webhook.
 */
public record Value(
    String messagingProduct,
    com.meta.chatbridge.message.webhook.whatsapp.Value.Metadata metadata,
    Collection<Contact> contacts,
    Collection<Error> errors,
    Collection<Status> statuses,
    Collection<WebhookMessage> messages) {

  @JsonCreator
  public Value(
      @JsonProperty("messaging_product") String messagingProduct,
      @JsonProperty("metadata") Metadata metadata,
      @JsonProperty("contacts") @Nullable Collection<Contact> contacts,
      @JsonProperty("errors") @Nullable Collection<Error> errors,
      @JsonProperty("statuses") @Nullable Collection<Status> statuses,
      @JsonProperty("messages") @Nullable Collection<WebhookMessage> messages) {
    Preconditions.checkArgument(Objects.equals(messagingProduct, "whatsapp"));
    this.metadata = Objects.requireNonNull(metadata);
    this.messagingProduct = messagingProduct;
    this.contacts = contacts == null ? Collections.emptyList() : contacts;
    this.errors = errors == null ? Collections.emptyList() : errors;
    this.statuses = statuses == null ? Collections.emptyList() : statuses;
    this.messages = messages == null ? Collections.emptyList() : messages;
  }

  /**
   * A metadata object describing the business subscribed to the webhook
   *
   * @param displayPhoneNumber The phone number that is displayed for a business
   * @param phoneNumberId ID for the phone number. A business can respond to a message using this ID
   */
  public record Metadata(String displayPhoneNumber, Identifier phoneNumberId) {
    @JsonCreator
    public Metadata(
        @JsonProperty("display_phone_number") String displayPhoneNumber,
        @JsonProperty("phone_number_id") String phoneNumberId) {
      this(
          Objects.requireNonNull(displayPhoneNumber),
          Identifier.from(Objects.requireNonNull(phoneNumberId)));
    }
  }
}
