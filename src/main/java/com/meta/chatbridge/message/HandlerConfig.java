/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = FBMessengerConfig.class, name = "messenger"),
  @JsonSubTypes.Type(value = WAMessengerConfig.class, name = "whatsapp"),
})
public interface HandlerConfig {
  String name();

  MessageHandler<?> toMessageHandler();
}
