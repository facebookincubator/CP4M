/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.hc.client5.http.utils.Hex;

class MetaHandlerUtils {
  static void subscriptionVerification(Context ctx, String verifyToken) {
    ctx.queryParamAsClass("hub.mode", String.class)
        .check(v -> v.equals("subscribe"), "hub.mode must be subscribe");
    ctx.queryParamAsClass("hub.verify_token", String.class)
        .check(v -> v.equals(verifyToken), "verify_token is incorrect");
    int challenge = ctx.queryParamAsClass("hub.challenge", int.class).get();
    ctx.result(String.valueOf(challenge));
  }

  static String hmac(String body, String appSecret) {
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

  /**
   * Use the appSecret to validate that the value set in X-Hub-Signature-256 is correct. Throws a
   * Javalin {@link io.javalin.validation.ValidationError} if the header is not valid
   *
   * <p><a
   * href="https://developers.facebook.com/docs/messenger-platform/reference/webhook-events">messenger
   * documentation on this process</a>
   *
   * @param ctx Javalin context corresponding to this post request
   * @param appSecret app secret corresponding to this app
   */
  static void postHeaderValidator(Context ctx, String appSecret) {
    ctx.headerAsClass("X-Hub-Signature-256", String.class)
        .check(
            h -> {
              String[] hashParts = h.strip().split("=");
              if (hashParts.length != 2) {
                return false;
              }
              String calculatedHmac = hmac(ctx.body(), appSecret);
              return hashParts[1].equals(calculatedHmac);
            },
            "X-Hub-Signature-256 could not be validated")
        .getOrThrow(ignored -> new ForbiddenResponse("X-Hub-Signature-256 could not be validated"));
  }
}
