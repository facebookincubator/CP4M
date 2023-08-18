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

public interface MessageHandler<T extends Message> {

  /**
   * Process incoming messages from users
   *
   * @param ctx incoming message from user
   * @return return a {@link Message} object
   */
  Optional<T> processRequest(Context ctx);

  void respond(T message);
}
