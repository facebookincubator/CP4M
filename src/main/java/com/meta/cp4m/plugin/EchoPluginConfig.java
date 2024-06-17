/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import com.meta.cp4m.message.Message;

public record EchoPluginConfig(String name) implements PluginConfig {

  @Override
  public <T extends Message> Plugin<T> toPlugin() {
    return new EchoPlugin<>();
  }
}
