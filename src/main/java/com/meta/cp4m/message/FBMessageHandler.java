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

  private final String verifyToken;
  private final String appSecret;

  private final String accessToken;
  private final boolean isInstagram;

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

  public FBMessageHandler(String verifyToken, String pageAccessToken, String appSecret, boolean isInstagram) {
    this.verifyToken = verifyToken;
    this.appSecret = appSecret;
    this.accessToken = pageAccessToken;
    this.isInstagram = isInstagram;
  }

  FBMessageHandler(FBMessengerConfig config) {
    this.verifyToken = config.verifyToken();
    this.appSecret = config.appSecret();
    this.accessToken = config.pageAccessToken();
    this.isInstagram = config.isInstagram();
  }

  @Override
  public List<FBMessage> processRequest(Context ctx) {
    try {
      switch (ctx.handlerType()) {
        case GET -> {
          return getHandler(ctx);
        }
        case POST -> {
          return postHandler(ctx);
        }
      }
    } catch (JsonProcessingException | NullPointerException e) {
      LOGGER
          .atWarn()
          .setMessage("Unable to parse message from Meta webhook")
          .setCause(e)
          .addKeyValue("body", ctx.body())
          .addKeyValue("headers", ctx.headerMap())
          .log();
      throw new BadRequestResponse("Invalid body");
    } catch (RuntimeException e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
    throw new UnsupportedOperationException("Only accepting get and post methods");
  }

  private List<FBMessage> getHandler(Context ctx) {
    MetaHandlerUtils.subscriptionVerification(ctx, verifyToken);
    LOGGER.debug("Meta verified callback url successfully");
    return Collections.emptyList();
  }

  @TestOnly
  String hmac(String body) {
    // TODO: refactor test so we don't need this
    return MetaHandlerUtils.hmac(body, appSecret);
  }

  private List<FBMessage> postHandler(Context ctx) throws JsonProcessingException {
    MetaHandlerUtils.postHeaderValidator(ctx, appSecret);

    String bodyString = ctx.body();
    JsonNode body = MAPPER.readTree(bodyString);
    String object = body.get("object").textValue();
    if (!object.equals("page") && !object.equals("instagram")) {
      LOGGER
          .atWarn()
          .setMessage("received body with value of " + object + " for 'object', expected 'page' or 'instagram'")
          .addKeyValue("body", bodyString)
          .log();
      return Collections.emptyList();
    }
    // TODO: need better validation
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
          if(messageObject.has("is_echo") && messageObject.get("is_echo").asText().equals("true")){
            return Collections.emptyList();
          }

          // https://developers.facebook.com/docs/messenger-platform/reference/webhook-events/messages
          Identifier messageId = Identifier.from(messageObject.get("mid").textValue());
          if (messageDeduplicator.addAndGetIsDuplicate(messageId)) {
            continue;
          }

          @Nullable JsonNode textObject = messageObject.get("text");
          if (textObject != null && textObject.isTextual()) {
            FBMessage m =
                new FBMessage(
                    timestamp,
                    messageId,
                    senderId,
                    recipientId,
                    textObject.textValue(),
                    Message.Role.USER);
            output.add(m);
          } else {
            LOGGER
                .atWarn()
                .setMessage("received message without text, unable to handle this")
                .addKeyValue("body", bodyString)
                .log();
          }
        } else {
          LOGGER
              .atWarn()
              .setMessage(
                  "received a message without a 'message' key, unable to handle this message type")
              .addKeyValue("body", bodyString)
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
          new URIBuilder(baseURLFactory.apply(isInstagram ? Identifier.from("me") : sender))
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
  public Collection<HandlerType> handlers() {
    return List.of(HandlerType.GET, HandlerType.POST);
  }
}
