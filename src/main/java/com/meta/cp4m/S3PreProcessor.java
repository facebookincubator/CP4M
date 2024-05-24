/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;
import com.meta.cp4m.message.FBMessage;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;


public class S3PreProcessor<T extends Message> implements PreProcessor<T> {
    private final S3Client s3Client;

    /**
     * @param in
     * @return
     */
    @Override
    public ThreadState<T> run(ThreadState<T> in) {
        // TODO: intercept the last message and redirect to upload it to S3
        return null;
    }


    public S3PreProcessor() {

        // Create session credentials
        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                "<key>",
                "<key>",
                ""
        );

        // Create a credentials provider
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(sessionCredentials);

        // Create an S3 client using the session credentials
        s3Client = S3Client.builder()
                .region(Region.US_EAST_2) // Specify your region (TODO: might be worth adding this to config)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    public void sendRequest() {
        String bucket = "<bucketName>";
        String key = "<key>";

        PutObjectResponse res = s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/pdf")
                        .build(),
                RequestBody.fromString("Testing"));

        s3Client.close();
    }
}


