/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meta.cp4m.message.Message;
import java.net.URI;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GenericPluginConfig implements PluginConfig {
  private final String name;
  private final URI url;
  private final AuthRequest authRequest;

  @JsonCreator
  public GenericPluginConfig(
      @JsonProperty("name") String name,
      @JsonProperty("url") String url,
      @JsonProperty("authentication") @Nullable AuthRequest authRequest) {
    this.name = Objects.requireNonNull(name, "name is a required parameter");
    this.url = URI.create(Objects.requireNonNull(url, "url is a required parameter"));
    this.authRequest = authRequest == null ? new AuthRequest.NoAuthRequest() : authRequest;
  }

  public URI url() {
    return url;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public <T extends Message> Plugin<T> toPlugin() {
    return new GenericPlugin<>(url, authRequest);
  }
}
