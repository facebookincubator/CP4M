/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import io.javalin.http.Context;
import java.util.Optional;

public class FBMessageHandler implements MessageHandler<FBMessage> {

  public FBMessageHandler() {}

  @Override
  public Optional<FBMessage> processRequest(Context ctx) {
    switch (ctx.handlerType()) {
      case GET -> {
        return getHandler();
      }
      case POST -> {
        return postHandler();
      }
      default -> throw new RuntimeException("Only accepting get and post methods");
    }
  }

  private Optional<FBMessage> getHandler() {
    return Optional.empty();
  }

  private Optional<FBMessage> postHandler() {
    return Optional.empty();
  }

  @Override
  public void respond(FBMessage message) {}
}
