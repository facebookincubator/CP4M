/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.meta.cp4m.message.Message;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = OpenAIConfig.class, name = "openai"),
  @JsonSubTypes.Type(value = HuggingFaceConfig.class, name = "hugging_face"),
  @JsonSubTypes.Type(value = EchoPluginConfig.class, name = "echo"),
  @JsonSubTypes.Type(value = GenericPluginConfig.class, name = "generic"),
})
public interface PluginConfig {

  String name();

  <T extends Message> Plugin<T> toPlugin();
}
