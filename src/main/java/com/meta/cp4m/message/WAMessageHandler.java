/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.webhook.whatsapp.TextWebhookMessage;
import com.meta.cp4m.message.webhook.whatsapp.Utils;
import com.meta.cp4m.message.webhook.whatsapp.WebhookMessage;
import com.meta.cp4m.message.webhook.whatsapp.WebhookPayload;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WAMessageHandler implements MessageHandler<WAMessage> {
  private static final String API_VERSION = "v17.0";
  private static final JsonMapper MAPPER = Utils.JSON_MAPPER;
  private static final Logger LOGGER = LoggerFactory.getLogger(WAMessageHandler.class);

  /**
   * <a
   * href="https://developers.facebook.com/docs/whatsapp/on-premises/reference/messages#constraints">A
   * text message can be a max of 4096 characters long.</a>
   */
  private static final int MAX_CHARS_PER_MESSAGE = 4096;

  private static final TextChunker CHUNKER = TextChunker.standard(MAX_CHARS_PER_MESSAGE);

  private final ExecutorService readExecutor = Executors.newCachedThreadPool();
  private final Deduplicator<Identifier> messageDeduplicator = new Deduplicator<>(10_000);
  private final String appSecret;
  private final String verifyToken;
  private final String accessToken;

  private Function<Identifier, URI> baseURLFactory =
      phoneNumberId -> {
        try {
          return new URIBuilder()
              .setScheme("https")
              .setHost("graph.facebook.com")
              .appendPath(API_VERSION)
              .appendPath(phoneNumberId.toString())
              .appendPath("messages")
              .build();
        } catch (URISyntaxException e) {
          // this should be impossible
          throw new RuntimeException(e);
        }
      };

  public WAMessageHandler(WAMessengerConfig config) {
    this.verifyToken = config.verifyToken();
    this.accessToken = config.accessToken();
    this.appSecret = config.appSecret();
  }

  private List<WAMessage> post(Context ctx, WebhookPayload payload) {
    System.out.println("Alert:: messages exchanged");
    List<WAMessage> waMessages = new ArrayList<>();
    payload.entry().stream()
        .flatMap(e -> e.changes().stream())
        .forEach(
            change -> {
              Identifier phoneNumberId = change.value().metadata().phoneNumberId();
              for (WebhookMessage message : change.value().messages()) {
                if (messageDeduplicator.addAndGetIsDuplicate(message.id())) {
                  continue; // message is a duplicate
                }
                if (message.type() != WebhookMessage.WebhookMessageType.TEXT) {
                  LOGGER.warn(
                      "received message of type '"
                          + message.type()
                          + "', only able to handle text messages at this time");
                  continue;
                }
                TextWebhookMessage textMessage = (TextWebhookMessage) message;
                waMessages.add(
                    new WAMessage(
                        message.timestamp(),
                        message.id(),
                        message.from(),
                        phoneNumberId,
                        textMessage.text().body(),
                        Message.Role.USER));
                readExecutor.execute(() -> markRead(phoneNumberId, textMessage.id().toString()));
              }
            });
    return waMessages;
  }

  @TestOnly
  @This
  WAMessageHandler baseUrlFactory(Function<Identifier, URI> baseURLFactory) {
    this.baseURLFactory = baseURLFactory;
    return this;
  }

  @Override
  public void respond(WAMessage message) throws IOException {
    for (String text : CHUNKER.chunks(message.message()).toList()) {
      send(message.recipientId(), message.senderId(), text);
    }
  }

  private void send(Identifier recipient, Identifier sender, String text) throws IOException {
    ObjectNode body =
        MAPPER
            .createObjectNode()
            .put("recipient_type", "individual")
            .put("messaging_product", "whatsapp")
            .put("type", "text")
            .put("to", recipient.toString());
    body.putObject("text").put("body", text);
    String bodyString;
    bodyString = MAPPER.writeValueAsString(body);
    Request.post(baseURLFactory.apply(sender))
        .setHeader("Authorization", "Bearer " + accessToken)
        .bodyString(bodyString, ContentType.APPLICATION_JSON)
        .execute();
  }

  @Override
  public List<RouteDetails<?, WAMessage>> routeDetails() {
    RouteDetails<WebhookPayload, WAMessage> postDetails =
        new RouteDetails<>(
            HandlerType.POST,
            ctx -> {
              @Nullable String contentType = ctx.contentType();
              if (contentType != null
                  && ContentType.parse(contentType).isSameMimeType(ContentType.APPLICATION_JSON)
                  && MetaHandlerUtils.postHeaderValid(ctx, appSecret)) {
                String bodyString = ctx.body();
                WebhookPayload payload;
                try {
                  payload = MAPPER.readValue(bodyString, WebhookPayload.class);
                  return Optional.of(payload);
                } catch (Exception e) {
                  return Optional.empty();
                }
              }
              return Optional.empty();
            },
            this::post);
    return List.of(MetaHandlerUtils.subscriptionVerificationRouteDetails(verifyToken), postDetails);
  }

  private void markRead(Identifier phoneNumberId, String messageId) {
    ObjectNode body =
        MAPPER
            .createObjectNode()
            .put("messaging_product", "whatsapp")
            .put("status", "read")
            .put("message_id", messageId);
    String bodyString;
    try {
      bodyString = MAPPER.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      // This should be impossible
      throw new RuntimeException(e);
    }

    try {
      Request.post(baseURLFactory.apply(phoneNumberId))
          .setHeader("Authorization", "Bearer " + accessToken)
          .bodyString(bodyString, ContentType.APPLICATION_JSON)
          .execute();
    } catch (IOException e) {
      // nothing we can do here, marking later messages as read will mark all previous messages read
      // so this is not a fatal issue
      LOGGER.error("unable to mark message as read", e);
    }
  }
}
