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
import java.util.Optional;

public interface MessageHandler<T extends Message> {

  /**
   * Process incoming requests from the messaging service, including messages from the user.
   *
   * @param ctx the context corresponding to an incoming request
   * @return return a {@link Message} object if appropriate
   */
  Optional<T> processRequest(Context ctx);

  /**
   * The method needed to respond to a message from a user
   *
   * @param message the response
   */
  void respond(T message);

  /**
   * @return The different {@link HandlerType}s that this handler expects to receive
   */
  Collection<HandlerType> handlers();
}
