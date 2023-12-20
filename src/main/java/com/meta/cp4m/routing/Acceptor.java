/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.meta.cp4m.message.Message;
import io.javalin.http.Context;

import java.util.Optional;

@FunctionalInterface
public interface Acceptor<T> {
  /**
   * @param ctx contex of an incoming message on a webhook
   * @return not empty if the {@link com.meta.cp4m.message.MessageHandler} can accept the message,
   *     empty otherwise
   */
  Optional<T> accept(Context ctx);
}
