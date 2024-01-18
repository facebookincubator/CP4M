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
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FBMessageHandler implements MessageHandler<FBMessage> {

  private static final String API_VERSION = "v17.0";
  private static final JsonMapper MAPPER = new JsonMapper();
  private static final Logger LOGGER = LoggerFactory.getLogger(FBMessageHandler.class);
  private static final TextChunker CHUNKER = TextChunker.standard(2000);

  private static final MessageFactory<FBMessage> MESSAGE_FACTORY = MessageFactory.instance(FBMessage.class);

  private final String verifyToken;
  private final String appSecret;

  private final String accessToken;
  private final @Nullable String connectedFacebookPageForInstagram;

  private final Deduplicator<Identifier> messageDeduplicator = new Deduplicator<>(10_000);
  private Function<Identifier, URI> baseURLFactory =
      pageId -> {
        try {
          return new URIBuilder()
              .setScheme("https")
              .setHost("graph.facebook.com")
              .appendPath(API_VERSION)
              .appendPath(pageId.toString())
              .appendPath("messages")
              .build();
        } catch (URISyntaxException e) {
          // this should be impossible
          throw new RuntimeException(e);
        }
      };

  public FBMessageHandler(
      String verifyToken,
      String pageAccessToken,
      String appSecret,
      @Nullable String connectedFacebookPageForInstagram) {
    this.verifyToken = verifyToken;
    this.appSecret = appSecret;
    this.accessToken = pageAccessToken;
    this.connectedFacebookPageForInstagram = connectedFacebookPageForInstagram;
  }

  public FBMessageHandler(String verifyToken, String pageAccessToken, String appSecret) {
    this.verifyToken = verifyToken;
    this.appSecret = appSecret;
    this.accessToken = pageAccessToken;
    this.connectedFacebookPageForInstagram = null;
  }

  FBMessageHandler(FBMessengerConfig config) {
    this.verifyToken = config.verifyToken();
    this.appSecret = config.appSecret();
    this.accessToken = config.pageAccessToken();
    this.connectedFacebookPageForInstagram =
        config.connectedFacebookPageForInstagram().isPresent()
            ? config.connectedFacebookPageForInstagram().get()
            : null;
  }

  @TestOnly
  String hmac(String body) {
    // TODO: refactor test so we don't need this
    return MetaHandlerUtils.hmac(body, appSecret);
  }

  private List<FBMessage> postHandler(Context ctx, JsonNode body) {
    JsonNode entries = body.get("entry");
    ArrayList<FBMessage> output = new ArrayList<>();
    for (JsonNode entry : entries) {
      @Nullable JsonNode messaging = entry.get("messaging");
      if (messaging == null) {
        continue;
      }
      for (JsonNode message : messaging) {

        Identifier senderId = Identifier.from(message.get("sender").get("id").asLong());
        Identifier recipientId = Identifier.from(message.get("recipient").get("id").asLong());
        Instant timestamp = Instant.ofEpochMilli(message.get("timestamp").asLong());
        @Nullable JsonNode messageObject = message.get("message");
        if (messageObject != null) {
          if (messageObject.has("is_echo")
              && messageObject.get("is_echo").asText().equals("true")) {
            return Collections.emptyList();
          }

          // https://developers.facebook.com/docs/messenger-platform/reference/webhook-events/messages
          Identifier messageId = Identifier.from(messageObject.get("mid").textValue());
          if (messageDeduplicator.addAndGetIsDuplicate(messageId)) {
            continue;
          }

          @Nullable JsonNode textObject = messageObject.get("text");
          if (textObject != null && textObject.isTextual()) {
            FBMessage m = MESSAGE_FACTORY.newMessage(timestamp, textObject.textValue(), senderId, recipientId,messageId, Message.Role.USER,null);
            output.add(m);
          } else {
            LOGGER
                .atWarn()
                .setMessage("received message without text, unable to handle this")
                .addKeyValue("body", body)
                .log();
          }
        } else {
          LOGGER
              .atWarn()
              .setMessage(
                  "received a message without a 'message' key, unable to handle this message type")
              .addKeyValue("body", body)
              .log();
        }
      }
    }

    return output;
  }

  @TestOnly
  public @This FBMessageHandler baseURLFactory(Function<Identifier, URI> baseURLFactory) {
    this.baseURLFactory = Objects.requireNonNull(baseURLFactory);
    return this;
  }

  @Override
  public void respond(FBMessage message) throws IOException {
    List<String> chunkedText = CHUNKER.chunks(message.message()).toList();
    for (String text : chunkedText) {
      send(text, message.recipientId(), message.senderId());
    }
  }

  private void send(String message, Identifier recipient, Identifier sender) throws IOException {
    URI url;
    ObjectNode body = MAPPER.createObjectNode();
    body.put("messaging_type", "RESPONSE").putObject("recipient").put("id", recipient.toString());
    body.putObject("message").put("text", message);
    String bodyString;
    try {
      bodyString = MAPPER.writeValueAsString(body);
      url =
          new URIBuilder(
                  baseURLFactory.apply(
                      connectedFacebookPageForInstagram == null
                          ? sender
                          : Identifier.from(connectedFacebookPageForInstagram)))
              .addParameter("access_token", accessToken)
              .build();
    } catch (JsonProcessingException | URISyntaxException e) {
      // should be impossible
      throw new RuntimeException(e);
    }

    Response response =
        Request.post(url).bodyString(bodyString, ContentType.APPLICATION_JSON).execute();
    HttpResponse responseContent = response.returnResponse();
    if (responseContent.getCode() != 200) {
      String errorMessage =
          "received a "
              + responseContent.getCode()
              + " error code when attempting to reply. "
              + responseContent.getReasonPhrase();

      LOGGER.atError().addKeyValue("body", bodyString).setMessage(errorMessage).log();
      throw new IOException(
          "received a "
              + responseContent.getCode()
              + " error code when attempting to reply. "
              + responseContent.getReasonPhrase());
    }
  }

  @Override
  public List<RouteDetails<?, FBMessage>> routeDetails() {
    RouteDetails<JsonNode, FBMessage> postDetails =
        new RouteDetails<>(
            HandlerType.POST,
            ctx -> {
              @Nullable String contentType = ctx.contentType();
              if (contentType != null
                  && ContentType.parse(contentType).isSameMimeType(ContentType.APPLICATION_JSON)
                  && MetaHandlerUtils.postHeaderValid(ctx, appSecret)) {
                JsonNode body;
                try {
                  body = MAPPER.readTree(ctx.body());
                } catch (JsonProcessingException e) {
                  throw new BadRequestResponse("unable to parse body");
                }
                // TODO: need better validation
                String expectedObjectValue =
                    connectedFacebookPageForInstagram == null ? "page" : "instagram";
                @Nullable JsonNode objectNode = body.get("object");
                if (objectNode != null && objectNode.textValue().equals(expectedObjectValue)) {
                  return Optional.of(body);
                }
              }
              return Optional.empty();
            },
            this::postHandler);

    return List.of(MetaHandlerUtils.subscriptionVerificationRouteDetails(verifyToken), postDetails);
  }
}
