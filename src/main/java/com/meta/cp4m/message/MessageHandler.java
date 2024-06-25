/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.meta.cp4m.routing.Acceptor;
import io.javalin.http.HandlerType;
import java.io.IOException;
import java.util.List;

public interface MessageHandler<T extends Message> {
  record RouteDetails<IN, OUT extends Message>(
      HandlerType handlerType, Acceptor<IN> acceptor, RequestProcessor<IN, OUT> requestProcessor) {}

  /**
   * The method needed to respond to a message from a user
   *
   * @param message the response
   */
  ThreadState<T> respond(T message) throws IOException;

  List<RouteDetails<?, T>> routeDetails();
}
