/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.hc.client5.http.utils.Hex;
import org.checkerframework.checker.nullness.qual.Nullable;

class MetaHandlerUtils {

  /**
   * ONLY CALL FROM A STATIC CONTEXT
   *
   * <p>turns the checked exception into a unchecked runtime exception so this should only be used
   * in context where the uri string is known to be valid and will not cause an exception
   *
   * @param uri valid uri string
   * @return URI object from the string
   */
  static URI staticURI(String uri) {
    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      // This should be impossible
      throw new RuntimeException(e);
    }
  }

  static <T extends Message>
      MessageHandler.RouteDetails<Integer, T> subscriptionVerificationRouteDetails(
          String verifyToken) {
    return new MessageHandler.RouteDetails<>(
        HandlerType.GET,
        ctx ->
        // validateSubscription handles putting challenge into context response if it succeeds
        {
          if (Objects.equals(ctx.queryParam("hub.mode"), "subscribe")
              && Objects.equals(ctx.queryParam("hub.verify_token"), verifyToken)) {
            return Optional.of(ctx.queryParamAsClass("hub.challenge", Integer.class).get());
          }
          return Optional.empty();
        },
        (ctx, challenge) -> {
          ctx.result(String.valueOf(challenge));
          return List.of();
        });
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
  static boolean postHeaderValid(Context ctx, String appSecret) {
    @Nullable String sig = ctx.header("X-Hub-Signature-256");
    if (sig == null) {
      return false;
    }

    String[] hashParts = sig.strip().split("=");
    if (hashParts.length != 2) {
      return false;
    }

    String calculatedHmac = hmac(ctx.body(), appSecret);
    return hashParts[1].equals(calculatedHmac);
  }
}
