/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import java.util.Objects;

public class ServiceConfiguration {
  private final String webhookPath;
  private final String handler;
  private final String store;
  private final String plugin;

  @JsonCreator
  ServiceConfiguration(
      @JsonProperty("webhook_path") String webhookPath,
      @JsonProperty("handler") String handler,
      @JsonProperty("store") String store,
      @JsonProperty("plugin") String plugin) {
    Preconditions.checkArgument(
        webhookPath != null && webhookPath.startsWith("/"),
        "webhook_path must be present and it must start with a forward slash (/)");
    this.webhookPath = webhookPath;
    this.handler = Objects.requireNonNull(handler, "handler must be present");
    this.store = store;
    this.plugin = Objects.requireNonNull(plugin, "plugin must be present");
  }

  public String webhookPath() {
    return webhookPath;
  }

  public String handler() {
    return handler;
  }

  public String store() {
    return store;
  }

  public String plugin() {
    return plugin;
  }
}
