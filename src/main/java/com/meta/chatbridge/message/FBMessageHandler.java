/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meta.chatbridge.FBID;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.client5.http.utils.Hex;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FBMessageHandler implements MessageHandler<FBMessage> {

  private static final String API_VERSION = "v17.0";
  private static final JsonMapper MAPPER = new JsonMapper();
  private static final Logger LOGGER = LoggerFactory.getLogger(FBMessageHandler.class);
  private final String verifyToken;
  private final String appSecret;

  private final String accessToken;

  private final Deduplicator<JsonNode> bodyDeduplicator = new Deduplicator<>(10_000);
  private Function<FBID, URI> baseURLFactory =
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

  public FBMessageHandler(String verifyToken, String pageAccessToken, String appSecret) {
    this.verifyToken = verifyToken;
    this.appSecret = appSecret;
    this.accessToken = pageAccessToken;
  }

  @TestOnly
  FBMessageHandler baseURLFactory(Function<FBID, URI> baseURLFactory) {
    this.baseURLFactory = Objects.requireNonNull(baseURLFactory);
    return this;
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
        default -> throw new UnsupportedOperationException("Only accepting get and post methods");
      }
    } catch (JsonProcessingException | NullPointerException e) {
      LOGGER
          .atWarn()
          .setMessage("Unable to parse message form Meta webhook")
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
  }

  private List<FBMessage> getHandler(Context ctx) {
    ctx.queryParamAsClass("hub.mode", String.class)
        .check(v -> v.equals("subscribe"), "hub.mode must be subscribe");
    ctx.queryParamAsClass("hub.verify_token", String.class)
        .check(v -> v.equals(verifyToken), "verify_token is incorrect");
    int challenge = ctx.queryParamAsClass("hub.challenge", int.class).get();
    ctx.result(String.valueOf(challenge));
    return Collections.emptyList();
  }

  String hmac(String body) {
    Mac sha256HMAC;
    SecretKeySpec secretKey;
    try {
      sha256HMAC = Mac.getInstance("HmacSHA256");
      secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      sha256HMAC.init(secretKey);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e); // Algorithms guaranteed to exist
    }
    return Hex.encodeHexString(sha256HMAC.doFinal(body.getBytes(StandardCharsets.UTF_8)));
  }

  private List<FBMessage> postHandler(Context ctx) throws JsonProcessingException {
    // https://developers.facebook.com/docs/messenger-platform/reference/webhook-events

    ctx.headerAsClass("X-Hub-Signature-256", String.class)
        .check(
            h -> {
              String[] hashParts = h.strip().split("=");
              if (hashParts.length != 2) {
                return false;
              }
              String calculatedHmac = hmac(ctx.body());
              return hashParts[1].equals(calculatedHmac);
            },
            "X-Hub-Signature-256 could not be validated")
        .get();

    JsonNode body = MAPPER.readTree(ctx.body());
    String object = body.get("object").textValue();
    if (!object.equals("page")) {
      LOGGER
          .atWarn()
          .setMessage("received body that has a different value for 'object' than 'page'")
          .addKeyValue("body", ctx.body())
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

        if (bodyDeduplicator.addAndGetIsDuplicate(message)) {
          continue;
        }

        FBID senderId = FBID.from(message.get("sender").get("id").asLong());
        FBID recipientId = FBID.from(message.get("recipient").get("id").asLong());
        Instant timestamp = Instant.ofEpochMilli(message.get("timestamp").asLong());
        @Nullable JsonNode messageObject = message.get("message");
        if (messageObject != null) {
          String messageId = messageObject.get("mid").textValue();
          String messageText = messageObject.get("text").textValue();
          FBMessage m =
              new FBMessage(
                  timestamp, messageId, senderId, recipientId, messageText, Message.Role.USER);
          output.add(m);
        }
      }
    }

    return output;
  }

  @Override
  public void respond(FBMessage message) throws IOException {
    URI url;
    ObjectNode body = MAPPER.createObjectNode();
    body.put("messaging_type", "RESPONSE")
        .putObject("recipient")
        .put("id", message.recipientId().toString());
    body.putObject("message").put("text", message.message());
    String bodyString;
    try {
      bodyString = MAPPER.writeValueAsString(body);
      url =
          new URIBuilder(baseURLFactory.apply(message.senderId()))
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
