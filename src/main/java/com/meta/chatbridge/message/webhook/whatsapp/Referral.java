/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message.webhook.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Referral(
    @JsonProperty("source_url") String sourceUrl,
    @JsonProperty("source_type") String sourceType,
    @JsonProperty("source_id") String sourceID,
    String headline,
    String body,
    @JsonProperty("media_type") String mediaType,
    @JsonProperty("image_url") String imageUrl,
    @JsonProperty("video_url") String videoUrl,
    @JsonProperty("thumbnail_url") String thumbnailUrl,
    @JsonProperty("ctwa_clid") String ctwaClid) {}
