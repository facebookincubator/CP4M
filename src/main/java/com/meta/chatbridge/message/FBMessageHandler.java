/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class FBMessageHandler implements MessageHandler<FBMessage> {

  private final String verifyToken;

  public FBMessageHandler(String verifyToken) {
    this.verifyToken = verifyToken;
  }

  @Override
  public Optional<FBMessage> processRequest(Context ctx) {
    switch (ctx.handlerType()) {
      case GET -> {
        return getHandler(ctx);
      }
      case POST -> {
        return postHandler(ctx);
      }
      default -> throw new UnsupportedOperationException("Only accepting get and post methods");
    }
  }

  private Optional<FBMessage> getHandler(Context ctx) {
    ctx.queryParamAsClass("hub.mode", String.class)
        .check(v -> v.equals("subscribe"), "hub.mode must be subscribe");
    ctx.queryParamAsClass("hub.verify_token", String.class)
        .check(v -> v.equals(verifyToken), "verify_token is incorrect");
    int challenge = ctx.queryParamAsClass("hub.challenge", int.class).get();
    ctx.result(String.valueOf(challenge));
    return Optional.empty();
  }

  private Optional<FBMessage> postHandler(Context ctx) {
    return Optional.empty();
  }

  @Override
  public void respond(FBMessage message) {}

  @Override
  public Collection<HandlerType> handlers() {
    return List.of(HandlerType.GET, HandlerType.POST);
  }
}
