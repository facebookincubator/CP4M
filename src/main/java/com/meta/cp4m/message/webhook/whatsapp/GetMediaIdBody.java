/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message.webhook.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public record GetMediaIdBody(
    @JsonProperty("messaging_product") String messagingProduct,
    @JsonProperty("mime_type") String mimeType,
    @JsonProperty("url") URI url,
    @JsonProperty("sha256") String sha256,
    @JsonProperty("file_size") String fileSize,
    @JsonProperty("id") String id) {}
