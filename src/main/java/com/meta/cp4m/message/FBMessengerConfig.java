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

public class FBMessengerConfig implements HandlerConfig {

  private final String name;
  private final String verifyToken;
  private final String appSecret;
  private final String pageAccessToken;

  public FBMessengerConfig(
      @JsonProperty("name") String name,
      @JsonProperty("verify_token") String verifyToken,
      @JsonProperty("app_secret") String appSecret,
      @JsonProperty("page_access_token") String pageAccessToken) {

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
  }


  public static FBMessengerConfig of(String verifyToken, String appSecret, String pageAccessToken) {
    // human readability of the name only matters when it's coming from a config
    return new FBMessengerConfig(
        UUID.randomUUID().toString(), verifyToken, appSecret, pageAccessToken);
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
  } // EAAICn2ZAELtYBO2pwykJbOqqEra8msZAjp98AZCFDr5raSYQBgkG5RVq40N0txHFnxMuPdmZArB3gY2R6YwQEk8Gk1LDeO98DZCZCmbyLg6S6conUSBdRgGTz99fBIPtn41ZAhSulBhQ5cg5Fu3Tw1zjlrnUZCNp2sBMuTZAsp1C3jfrIEMbMvZBR9ml7iksNbRZCFrEjvUlWeyZAPJsAiPdQyeRM00sqCDqSya2hB8R7QIc
} // EAAICn2ZAELtYBO3tZB7c3sObbkw6aHxrkCuZA7k2ECfZAunCJVkrAmgbvlDSAEMRjLl4FDPOiuBkZCkGYayMZBJrGCPJDkgdBqcf21StChoE4uOexspURdDiZCPTtdcxXGljsT4Xh0uieoM19M2Evm7O5LWjKPluRoGwUZBZC4lX4iF3TEw8JCa9npkIRnV823ZCvOyPP9QBo2uGScZCX1ZA

