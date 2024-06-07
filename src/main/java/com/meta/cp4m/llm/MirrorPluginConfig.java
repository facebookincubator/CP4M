/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import com.meta.cp4m.message.Message;

public record MirrorPluginConfig(String name) implements LLMConfig {

  @Override
  public <T extends Message> LLMPlugin<T> toPlugin() {
    return new MirrorPlugin<>();
  }
}
