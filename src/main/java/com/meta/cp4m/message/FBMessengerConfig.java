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
import java.util.Optional;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FBMessengerConfig implements HandlerConfig {

  private final String name;
  private final String verifyToken;
  private final String appSecret;
  private final String pageAccessToken;
  private final @Nullable String connectedFacebookPageForInstagram;

  private FBMessengerConfig(
      @JsonProperty("name") String name,
      @JsonProperty("verify_token") String verifyToken,
      @JsonProperty("app_secret") String appSecret,
      @JsonProperty("page_access_token") String pageAccessToken,
      @JsonProperty("connected_facebook_page_for_instagram")
          @Nullable String connectedFacebookPageForInstagram) {

    Preconditions.checkArgument(name != null && !name.isBlank(), "name cannot be blank");
    Preconditions.checkArgument(
        verifyToken != null && !verifyToken.isBlank(), "verify_token cannot be blank");
    Preconditions.checkArgument(
        appSecret != null && !appSecret.isBlank(), "app_secret cannot be blank");
    Preconditions.checkArgument(
        pageAccessToken != null && !pageAccessToken.isBlank(), "page_access_token cannot be blank");

    this.name = name;
    this.verifyToken = verifyToken;
    this.appSecret = appSecret;
    this.pageAccessToken = pageAccessToken;
    this.connectedFacebookPageForInstagram = connectedFacebookPageForInstagram;
  }

  public static FBMessengerConfig of(
      String verifyToken,
      String appSecret,
      String pageAccessToken,
      @Nullable String connectedFacebookPageForInstagram) {
    // human readability of the name only matters when it's coming from a config
    return new FBMessengerConfig(
        UUID.randomUUID().toString(),
        verifyToken,
        appSecret,
        pageAccessToken,
        connectedFacebookPageForInstagram);
  }

  public static FBMessengerConfig of(String verifyToken, String appSecret, String pageAccessToken) {
    // human readability of the name only matters when it's coming from a config
    return new FBMessengerConfig(
        UUID.randomUUID().toString(), verifyToken, appSecret, pageAccessToken, null);
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
  public FBMessageHandler toMessageHandler() {
    return new FBMessageHandler(this);
  }

  public String pageAccessToken() {
    return pageAccessToken;
  }

  public Optional<String> connectedFacebookPageForInstagram() {
    return Optional.ofNullable(connectedFacebookPageForInstagram);
  }
}
