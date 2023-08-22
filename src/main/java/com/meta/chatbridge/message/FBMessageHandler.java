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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meta.chatbridge.FBID;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.hc.client5.http.utils.Hex;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FBMessageHandler implements MessageHandler<FBMessage> {

  private static final JsonMapper MAPPER = new JsonMapper();

  private final String verifyToken;
  private final @Nullable String appSecret;

  public FBMessageHandler(String verifyToken, @Nullable String appSecret) {
    this.verifyToken = verifyToken;
    this.appSecret = appSecret;
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
    } catch (Exception e) {
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

  private String hmac(String body) {
    Objects.requireNonNull(appSecret);
    Mac sha256_HMAC;
    SecretKeySpec secret_key;
    try {
      sha256_HMAC = Mac.getInstance("HmacSHA256");
      secret_key = new SecretKeySpec(appSecret.getBytes("UTF-8"), "HmacSHA256");
      sha256_HMAC.init(secret_key);
      return Hex.encodeHexString(sha256_HMAC.doFinal(body.getBytes("UTF-8")));
    } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e); // Algorithms guaranteed to exist
    }
  }

  private List<FBMessage> postHandler(Context ctx) throws JsonProcessingException {

    ctx.headerAsClass("x-hub-signature-256", String.class)
        .check(
            h -> {
              if (appSecret == null) {
                return true;
              }
              String[] hashParts = h.strip().split("=");
              if (hashParts.length != 2) {
                return false;
              }
              String calculatedHmac = hmac(ctx.body());
              return hashParts[1].equals(calculatedHmac);
            },
            "x-hub-signature-256 could not be validated");

    ObjectNode body = (ObjectNode) MAPPER.readTree(ctx.body());
    String object = body.get("object").asText();
    if (!object.equals("page")) {
      return Collections.emptyList();
    }
    // TODO: need better validation
    ArrayNode entries = (ArrayNode) body.get("entry");
    ArrayList<FBMessage> output = new ArrayList<>();
    for (JsonNode entry : entries) {
      @Nullable JsonNode messaging = entry.get("messaging");
      if (entry.get("messaging") == null) {
        continue;
      }
      for (JsonNode message : messaging) {
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
  public void respond(FBMessage message) {}

  @Override
  public Collection<HandlerType> handlers() {
    return List.of(HandlerType.GET, HandlerType.POST);
  }
}
