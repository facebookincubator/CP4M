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
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public record SendResponse(
    String messagingProduct,
    List<SendResponseContact> contacts,
    List<SendResponseMessages> messages) {
  @JsonCreator
  public SendResponse(
      @JsonProperty("messaging_product") String messagingProduct,
      @JsonProperty("contacts") List<SendResponseContact> contacts,
      @JsonProperty("messages") List<SendResponseMessages> messages) {
    this.messagingProduct = messagingProduct;
    this.contacts = contacts == null ? Collections.emptyList() : contacts;
    this.messages = messages == null ? Collections.emptyList() : messages;
  }

  @Override
  public String toString() {
    return "SendResponse["
        + "messagingProduct="
        + messagingProduct
        + ", "
        + "contacts="
        + contacts
        + ", "
        + "messages="
        + messages
        + ']';
  }

  public record SendResponseContact(
      @JsonProperty("input") String phoneNumber, @JsonProperty("wa_id") String phoneNumberId) {}

  public record SendResponseMessages(
      @JsonProperty("id") String messageId,
      @Nullable @JsonProperty("message_status") String messageStatus) {}
}
