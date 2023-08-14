/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.fasterxml.jackson.databind.JsonNode;

public interface MessageHandler<T extends Message> {
  T processRequest(JsonNode request);

  void respond(T message);
}
