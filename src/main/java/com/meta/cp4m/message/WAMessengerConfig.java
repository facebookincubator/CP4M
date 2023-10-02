/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import java.util.UUID;

public class WAMessengerConfig implements HandlerConfig {

  private final String name;
  private final String verifyToken;
  private final String appSecret;
  private final String accessToken;

  private WAMessengerConfig(
      @JsonProperty("name") String name,
      @JsonProperty("verify_token") String verifyToken,
      @JsonProperty("app_secret") String appSecret,
      @JsonProperty("access_token") String accessToken) {

    Preconditions.checkArgument(name != null && !name.isBlank(), "name cannot be blank");
    Preconditions.checkArgument(
        verifyToken != null && !verifyToken.isBlank(), "verify_token cannot be blank");
    Preconditions.checkArgument(
        appSecret != null && !appSecret.isBlank(), "app_secret cannot be blank");
    Preconditions.checkArgument(
        accessToken != null && !accessToken.isBlank(), "access_token cannot be blank");

    this.name = name;
    this.verifyToken = verifyToken;
    this.appSecret = appSecret;
    this.accessToken = accessToken;
  }

  public static WAMessengerConfig of(String verifyToken, String appSecret, String accessToken) {
    // human readability of the name only matters when it's coming from a config
    return new WAMessengerConfig(UUID.randomUUID().toString(), verifyToken, appSecret, accessToken);
  }

  @Override
  public String name() {
    return name;
  }

  public String verifyToken() {
    return verifyToken;
  }

  public String appSecret() {
    return appSecret;
  }

  @Override
  public WAMessageHandler toMessageHandler() {
    return new WAMessageHandler(this);
  }

  public String accessToken() {
    return accessToken;
  }
}
