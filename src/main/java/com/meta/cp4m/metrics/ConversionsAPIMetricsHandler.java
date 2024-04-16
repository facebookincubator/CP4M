/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.metrics;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

public class ConversionsAPIMetricsHandler {
    public static void main(String[] args) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.facebook.com/v19.0/1452308421990109/events?access_token=EAAKxnuilYZAQBO2dbiWA3r8z2FMzdmU2ZAfS3idlE1mzRXbZBJWDnZBmJNihs1CYz8GRZBl83jaErtsckFr8M88tWHUafREOEvFfDsq9vvSByks57y3sv5DZCa5IUqCSSjDwR4OJS2QkBogSvZAoCyO0KDJn69RzZBrQZCPeCk87PvfRf3yJP7gYQdL6H1dSD5BjSZCMN2EmgVM5aELrhGh3EZD"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString("{\n" +
                        "  \"data\": [\n" +
                        "    {\n" +
                        "      \"action_source\": \"website\",\n" +
                        "      \"event_id\": 12345,\n" +
                        "      \"event_name\": \"Working95\",\n" +
                        "      \"event_time\": 1713270881,\n" +
                        "      \"user_data\": {\n" +
                        "        \"em\": \"f660ab912ec121d1b1e928a0bb4bc61b15f5ad44d5efdc4e1c92a25e99b8e44a\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"test_event_code\": \"TEST19078\"\n" +
                        "}"))
                .build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
