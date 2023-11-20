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

public class IGMessengerConfig extends FBMessengerConfig {

    IGMessengerConfig( @JsonProperty("name") String name,
                       @JsonProperty("verify_token") String verifyToken,
                       @JsonProperty("app_secret") String appSecret,
                       @JsonProperty("page_access_token") String pageAccessToken){
        super(name, verifyToken, appSecret, pageAccessToken);
    }

    @Override
    public IGMessageHandler toMessageHandler() {
        return new IGMessageHandler(this);
    }

}
