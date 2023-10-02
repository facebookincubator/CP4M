/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.meta.cp4m.message.Message;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = OpenAIConfig.class, name = "openai"),
  @JsonSubTypes.Type(value = HuggingFaceConfig.class, name = "hugging_face"),
})
public interface LLMConfig {

  String name();

  <T extends Message> LLMPlugin<T> toPlugin();
}
