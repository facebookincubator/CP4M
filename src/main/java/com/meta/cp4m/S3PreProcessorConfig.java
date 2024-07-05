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
import com.meta.cp4m.message.Message;
import java.util.Objects;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

public record S3PreProcessorConfig(
    String name,
    String awsAccessKeyId,
    String awsSecretAccessKey,
    String region,
    String bucket,
    @Nullable String textMessageAddition)
    implements PreProcessorConfig {

  private static final Pattern KEBAB_CASE = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

  @JsonCreator
  public S3PreProcessorConfig(
      @JsonProperty("name") String name,
      @JsonProperty("aws_access_key_id") String awsAccessKeyId,
      @JsonProperty("aws_secret_access_key") String awsSecretAccessKey,
      @JsonProperty("region") String region,
      @JsonProperty("bucket") String bucket,
      @JsonProperty("text_message_addition") @Nullable String textMessageAddition) {

    Preconditions.checkArgument(
        KEBAB_CASE.matcher(bucket).matches(),
        "bucket does not match the aws region format(kebab case) or is empty");

    this.name = Objects.requireNonNull(name, "name is a required parameter");
    this.awsAccessKeyId =
        Objects.requireNonNull(awsAccessKeyId, "aws access key is a required parameter");
    this.awsSecretAccessKey =
        Objects.requireNonNull(awsSecretAccessKey, "aws secret access key is a required parameter");
    this.region = Objects.requireNonNull(region, "region is a required parameter");
    this.bucket = Objects.requireNonNull(bucket, "bucket is a required parameter");
    this.textMessageAddition = textMessageAddition;
  }

  @Override
  public <T extends Message> PreProcessor<T> toPreProcessor() {
    return new S3PreProcessor<>(
        awsAccessKeyId, awsSecretAccessKey, region, bucket, textMessageAddition);
  }
}
