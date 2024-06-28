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
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;

public class S3PreProcessor<T extends Message> implements PreProcessor<T> {
//    private final String aws_access_key_id;
//    private final String aws_secret_access_key;
    private S3Client s3Client;


    @Override
    public ThreadState<T> run(ThreadState<T> in) {
        // TODO: intercept the last message and redirect to upload it to S3
//        System.out.println(in.tail());
//        System.out.println(in.tail().payload().value());
        this.sendRequest((byte[]) in.tail().payload().value());
        System.out.println("Inside Run");
        return null;
    }


    public S3PreProcessor(
//            @JsonProperty("app_secret") String appSecret,
//            @JsonProperty("access_token") String accessToken
    ) {
        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                "testKey",
                "testsecret",
                ""
        );

        // Create a credentials provider
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(sessionCredentials);

        // Create an S3 client using the session credentials
        System.out.println("S3 client building...");
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_2) // Specify your region (TODO: might be worth adding this to config)
                .credentialsProvider(credentialsProvider)
                .build();
    }


    public void sendRequest(byte[] media) {
        String bucket = "cp4m-general-purpose-bucket-1";
        String key = "paper-7";
        System.out.println("Sending request to S3");
//        System.out.println(media);
        System.out.println(media.length);

        PutObjectResponse res = s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/pdf")
                        .build(),
                RequestBody.fromBytes(media));
        s3Client.close();
    }
}