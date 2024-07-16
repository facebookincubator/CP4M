/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.Payload;
import com.meta.cp4m.message.ThreadState;
import java.time.Instant;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

public class S3PreProcessor<T extends Message> implements PreProcessor<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3PreProcessor.class);
  private final String awsAccessKeyID;
  private final String awsSecretAccessKey;
  private final String region;
  private final String bucket;
  private final @Nullable String textMessageAddition;
  private final AwsCredentialsProvider credentials;

  public S3PreProcessor(
      String awsAccessKeyID,
      String awsSecretAccessKey,
      String region,
      String bucket,
      @Nullable String textMessageAddition) {
    this.awsAccessKeyID = awsAccessKeyID;
    this.awsSecretAccessKey = awsSecretAccessKey;
    this.region = region;
    this.bucket = bucket;
    this.textMessageAddition = textMessageAddition;

    @Nullable StaticCredentialsProvider staticCredentials;
    if (!this.awsAccessKeyID.isEmpty() && !this.awsSecretAccessKey.isEmpty()) {
      AwsSessionCredentials sessionCredentials =
          AwsSessionCredentials.create(this.awsAccessKeyID, this.awsSecretAccessKey, "");
      staticCredentials = StaticCredentialsProvider.create(sessionCredentials);
    } else {
      staticCredentials = null;
    }

    this.credentials =
        Objects.requireNonNullElse(staticCredentials, DefaultCredentialsProvider.create());
  }

  @Override
  public ThreadState<T> run(ThreadState<T> in) {

    switch (in.tail().payload()) {
      case Payload.Image i -> {
        LOGGER.atDebug().addKeyValue("payload", i).log("Received image payload");
        this.sendRequest(i.value(), in.userId().toString(), i.extension(), i.mimeType());
      }
      case Payload.Document i -> {
        LOGGER.atDebug().addKeyValue("payload", i).log("Received document payload");
        this.sendRequest(i.value(), in.userId().toString(), i.extension(), i.mimeType());
      }
      default -> {
        LOGGER.debug("Received text payload");
        return in;
      }
    }
    return textMessageAddition == null
        ? in
        : in.with(in.newMessageFromUser(Instant.now(), textMessageAddition, Identifier.random()));
  }

  public void sendRequest(byte[] media, String senderID, String extension, String mimeType) {
    String key = senderID + '_' + Instant.now().toEpochMilli() + '.' + extension;
    LOGGER.debug("attempting to upload \"" + key + "\" file to AWS S3");
    try (S3Client s3Client =
        S3Client.builder()
            .region(Region.of(this.region))
            .credentialsProvider(this.credentials)
            .build()) {

      PutObjectRequest request =
          PutObjectRequest.builder().bucket(this.bucket).key(key).contentType(mimeType).build();
      PutObjectResponse response = s3Client.putObject(request, RequestBody.fromBytes(media));
      LOGGER
          .atDebug()
          .addKeyValue("response", response)
          .addKeyValue("file", key)
          .log("AWS S3 response received on successful upload");
      LOGGER.info("Media upload to AWS S3 successful");
    } catch (Exception e) {
      LOGGER.error("Media upload to AWS S3 failed, {e}", e);
    }
  }
}
