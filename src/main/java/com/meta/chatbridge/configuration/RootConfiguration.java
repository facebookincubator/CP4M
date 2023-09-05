/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meta.chatbridge.llm.LLMConfig;
import java.util.Collection;
import java.util.Collections;

public class RootConfiguration {
  private final Collection<LLMConfig> plugins;

  @JsonCreator
  public RootConfiguration(@JsonProperty("plugins") Collection<LLMConfig> plugins) {
    this.plugins = plugins;
  }

  Collection<LLMConfig> plugins() {
    return Collections.unmodifiableCollection(plugins);
  }
}
