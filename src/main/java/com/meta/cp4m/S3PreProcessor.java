/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;

public class S3PreProcessor<T extends Message> implements PreProcessor<T> {
    private final String awsAccessKeyID;
    private final String awsSecretAccessKey;
    private final String region;
    private S3Client s3Client;
    private final String bucket;
    private static final Logger LOGGER = LoggerFactory.getLogger(S3PreProcessor.class);



    @Override
    public ThreadState<T> run(ThreadState<T> in) {
        if(in.tail().payload().getClass().getName().contains("Image") || in.tail().payload().getClass().getName().contains("Document")) {
            this.sendRequest((byte[]) in.tail().payload().value(), in.tail().senderId().toString());
        }
        return null;
    }

    public S3PreProcessor(String awsAccessKeyID, String awsSecretAccessKey, String region, String bucket) {
        this.awsAccessKeyID = awsAccessKeyID;
        this.awsSecretAccessKey = awsSecretAccessKey;
        this.region = region;
        this.bucket = bucket;

        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                this.awsAccessKeyID,
                this.awsSecretAccessKey,
                ""
        );

        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(sessionCredentials);
        this.s3Client = S3Client.builder()
                // TODO: Add check to make sure the region is in kebab case
                .region(Region.of(this.region))
                .credentialsProvider(credentialsProvider)
                .build();
    }


    public void sendRequest(byte[] media, String senderID) {
        String key = senderID + "_" + Instant.now().toEpochMilli();
        PutObjectResponse res = s3Client.putObject(PutObjectRequest.builder().bucket(this.bucket).key(key).contentType("application/*")
                        .build(),
                RequestBody.fromBytes(media));
        s3Client.close();
        LOGGER.info("Media uploaded to AWS S3");

    }
}