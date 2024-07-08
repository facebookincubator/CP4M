/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.webhook.whatsapp.*;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WAMessageHandler implements MessageHandler<WAMessage> {
  private static final String API_VERSION = "v19.0";
  private static final JsonMapper MAPPER = Utils.JSON_MAPPER;
  private static final Logger LOGGER = LoggerFactory.getLogger(WAMessageHandler.class);

  /**
   * <a
   * href="https://developers.facebook.com/docs/whatsapp/on-premises/reference/messages#constraints">A
   * text message can be a max of 4096 characters long.</a>
   */
  private static final int MAX_CHARS_PER_MESSAGE = 4096;

  private static final URI DEFAULT_BASE_URI =
      MetaHandlerUtils.staticURI("https://graph.facebook.com/" + API_VERSION);

  private static final TextChunker CHUNKER = TextChunker.standard(MAX_CHARS_PER_MESSAGE);

  private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
  private final Deduplicator<Identifier> messageDeduplicator = new Deduplicator<>(10_000);
  private final String appSecret;
  private final String verifyToken;
  private final String accessToken;
  private final String appSecretProof;
  private final @Nullable String welcomeMessage;
  private URI baseURL = DEFAULT_BASE_URI;

  public WAMessageHandler(WAMessengerConfig config) {
    this.verifyToken = config.verifyToken();
    this.accessToken = config.accessToken();
    this.appSecret = config.appSecret();
    this.welcomeMessage = config.welcomeMessage().orElse(null);
    this.appSecretProof = MetaHandlerUtils.hmac(accessToken, appSecret);
  }

  private List<ThreadState<WAMessage>> post(Context ctx, WebhookPayload payload) {
    List<ThreadState<WAMessage>> threadStates = new ArrayList<>();
    payload.entry().stream()
        .flatMap(e -> e.changes().stream())
        .forEachOrdered(
            change -> {
              Identifier phoneNumberId = change.value().metadata().phoneNumberId();
              for (WebhookMessage message : change.value().messages()) {
                if (messageDeduplicator.addAndGetIsDuplicate(message.id())) {
                  continue; // message is a duplicate
                }
                Payload<?> payloadValue;
                switch (message) {
                  case TextWebhookMessage m -> payloadValue = new Payload.Text(m.text().body());
                  case ImageWebhookMessage m -> {
                    try {
                      URI url = this.getUrlFromID(m.image().id());
                      byte[] media = this.getMediaFromUrl(url);
                      payloadValue = new Payload.Image(media, m.image().mimeType());
                    } catch (IOException | URISyntaxException e) {
                      throw new RuntimeException(e);
                    }
                  }

                  case DocumentWebhookMessage m -> {
                    try {
                      URI url = this.getUrlFromID(m.document().id());
                      byte[] media = this.getMediaFromUrl(url);
                      payloadValue = new Payload.Document(media, m.document().mimeType());
                    } catch (IOException | URISyntaxException e) {
                      throw new RuntimeException(e);
                    }
                  }

                  case WelcomeWebhookMessage ignored -> {
                    if (welcomeMessage != null) {
                      WAMessage welcome =
                          new WAMessage(
                              message.timestamp(),
                              message.id(),
                              message.from(),
                              phoneNumberId,
                              welcomeMessage,
                              Message.Role.ASSISTANT);
                      asyncExecutor.submit(
                          () -> {
                            try {
                              respond(welcome);
                            } catch (IOException e) {
                              LOGGER.error("unable to send welcome message");
                            }
                          });
                    }
                    continue;
                  }
                  default -> {
                    LOGGER.warn(
                        "received message of type '"
                            + message.type()
                            + "', only able to handle text at this time");
                    continue;
                  }
                }
                ThreadState<WAMessage> ts =
                    ThreadState.of(
                        new WAMessage(
                            message.timestamp(),
                            message.id(),
                            message.from(),
                            phoneNumberId,
                            payloadValue,
                            Message.Role.USER));
                UserData userData =
                    change.value().contacts().stream() // should only ever be one contact
                        .map(Contact::profile)
                        .map(p -> ts.userData().withName(p.name()))
                        .findAny()
                        .orElse(ts.userData());
                threadStates.add(ts.withUserData(userData));
                asyncExecutor.execute(() -> markRead(phoneNumberId, message.id().toString()));
              }
            });
    return threadStates;
  }

  @TestOnly
  @This
  WAMessageHandler baseUrl(URI baseURL) {
    this.baseURL = baseURL;
    return this;
  }

  @Override
  public ThreadState<WAMessage> respond(WAMessage message) throws IOException {
    if (!(message.payload() instanceof Payload.Text)) {
      throw new UnsupportedOperationException(
          "Non-text payloads cannot be sent to Whatsapp client currently");
    }
    @Nullable List<SendResponse> responses = new ArrayList<>();
    for (String text : CHUNKER.chunks(message.message()).toList()) {
      SendResponse r = send(message.recipientId(), message.senderId(), text);
      responses.add(r);
    }
    ThreadState<WAMessage> ts = ThreadState.of(message);
    return responses.stream()
        .map(SendResponse::contacts)
        .flatMap(List::stream)
        .findAny()
        .map(SendResponse.SendResponseContact::phoneNumber)
        .map(ph -> ts.userData().withPhoneNumber(ph))
        .map(ts::withUserData)
        .orElse(ts);
  }

  private URI messagesURI(Identifier phoneNumberId) {
    try {
      return new URIBuilder(baseURL)
          .appendPath(phoneNumberId.toString())
          .appendPath("messages")
          .build();
    } catch (URISyntaxException e) {
      // should be impossible
      throw new RuntimeException(e);
    }
  }

  SendResponse send(Identifier recipient, Identifier sender, String text) throws IOException {
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
    return Request.post(messagesURI(sender))
        .setHeader("Authorization", "Bearer " + accessToken)
        .setHeader("appsecret_proof", appSecretProof)
        .bodyString(bodyString, ContentType.APPLICATION_JSON)
        .execute()
        .handleResponse(
            response -> {
              try {
                return MAPPER.readValue(response.getEntity().getContent(), SendResponse.class);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
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
      Request.post(messagesURI(phoneNumberId))
          .setHeader("Authorization", "Bearer " + accessToken)
          .setHeader("appsecret_proof", appSecretProof)
          .bodyString(bodyString, ContentType.APPLICATION_JSON)
          .execute()
          .discardContent();
    } catch (IOException e) {
      // nothing we can do here, marking later messages as read will mark all previous messages read
      // so this is not a fatal issue
      LOGGER.error("unable to mark message as read", e);
    }
  }

  private URI getUrlFromID(String mediaID) throws IOException, URISyntaxException {
    return Request.get(new URIBuilder(this.baseURL).appendPath(mediaID).build())
        .setHeader("Authorization", "Bearer " + accessToken)
        .setHeader("appsecret_proof", appSecretProof)
        .execute()
        .handleResponse(
            response -> {
              try {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = MAPPER.readTree(jsonResponse);
                return new URIBuilder(jsonNode.get("url").asText());
              } catch (URISyntaxException e) {
                throw new RuntimeException(e);
              }
            })
        .build();
  }

  private byte[] getMediaFromUrl(URI url) throws IOException {
    return Request.get(url)
        .setHeader("Authorization", "Bearer " + accessToken)
        .setHeader("appsecret_proof", appSecretProof)
        .execute()
        .handleResponse(
            response -> {
              try {
                return EntityUtils.toByteArray(response.getEntity());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }
}
