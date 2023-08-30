/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Copyright 2020 Alex Vasiliev, licensed under the Apache 2.0 license: https://opensource.org/licenses/Apache-2.0
 */
public class AmazonRequestSignatureV4Utils {

    /**
     * Generates signing headers for HTTP request in accordance with Amazon AWS API Signature version 4 process.
     * <p>
     * Following steps outlined here: <a href="https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html">docs.aws.amazon.com</a>
     * <p>
     * This method takes many arguments as read-only, but adds necessary headers to @{code headers} argument, which is a map.
     * The caller should make sure those parameters are copied to the actual request object.
     * <p>
     * The ISO8601 date parameter can be created by making a call to:<br>
     * - {@code java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").format(ZonedDateTime.now(ZoneOffset.UTC))}<br>
     * or, if you prefer joda:<br>
     * - {@code org.joda.time.format.ISODateTimeFormat.basicDateTimeNoMillis().print(DateTime.now().withZone(DateTimeZone.UTC))}
     *
     * @param method - HTTP request method, (GET|POST|DELETE|PUT|...), e.g., {@link java.net.HttpURLConnection#getRequestMethod()}
     * @param host - URL host, e.g., {@link java.net.URL#getHost()}.
     * @param path - URL path, e.g., {@link java.net.URL#getPath()}.
     * @param query - URL query, (parameters in sorted order, see the AWS spec) e.g., {@link java.net.URL#getQuery()}.
     * @param headers - HTTP request header map. This map is going to have entries added to it by this method. Initially populated with
     *     headers to be included in the signature. Like often compulsory 'Host' header. e.g., {@link java.net.HttpURLConnection#getRequestProperties()}.
     * @param body - The binary request body, for requests like POST.
     * @param isoDateTime - The time and date of the request in ISO8601 basic format, see comment above.
     * @param awsIdentity - AWS Identity, e.g., "AKIAJTOUYS27JPVRDUYQ"
     * @param awsSecret - AWS Secret Key, e.g., "I8Q2hY819e+7KzBnkXj66n1GI9piV+0p3dHglAzQ"
     * @param awsRegion - AWS Region, e.g., "us-east-1"
     * @param awsService - AWS Service, e.g., "route53"
     */
    public static void calculateAuthorizationHeaders(
            String method, String host, String path, String query, Map<String, String> headers,
            byte[] body,
            String isoDateTime,
            String awsIdentity, String awsSecret, String awsRegion, String awsService
    ) {
        try {
            String bodySha256 = hex(sha256(body));
            String isoJustDate = isoDateTime.substring(0, 8); // Cut the date portion of a string like '20150830T123600Z';

            headers.put("Host", host);
            headers.put("X-Amz-Content-Sha256", bodySha256);
            headers.put("X-Amz-Date", isoDateTime);

            // (1) https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
            List<String> canonicalRequestLines = new ArrayList<>();
            canonicalRequestLines.add(method);
            canonicalRequestLines.add(path);
            canonicalRequestLines.add(query);
            List<String> hashedHeaders = new ArrayList<>();
            List<String> headerKeysSorted = headers.keySet().stream().sorted(Comparator.comparing(e -> e.toLowerCase(Locale.US))).collect(Collectors.toList());
            for (String key : headerKeysSorted) {
                hashedHeaders.add(key.toLowerCase(Locale.US));
                canonicalRequestLines.add(key.toLowerCase(Locale.US) + ":" + normalizeSpaces(headers.get(key)));
            }
            canonicalRequestLines.add(null); // new line required after headers
            String signedHeaders = hashedHeaders.stream().collect(Collectors.joining(";"));
            canonicalRequestLines.add(signedHeaders);
            canonicalRequestLines.add(bodySha256);
            String canonicalRequestBody = canonicalRequestLines.stream().map(line -> line == null ? "" : line).collect(Collectors.joining("\n"));
            String canonicalRequestHash = hex(sha256(canonicalRequestBody.getBytes(StandardCharsets.UTF_8)));

            // (2) https://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html
            List<String> stringToSignLines = new ArrayList<>();
            stringToSignLines.add("AWS4-HMAC-SHA256");
            stringToSignLines.add(isoDateTime);
            String credentialScope = isoJustDate + "/" + awsRegion + "/" + awsService + "/aws4_request";
            stringToSignLines.add(credentialScope);
            stringToSignLines.add(canonicalRequestHash);
            String stringToSign = stringToSignLines.stream().collect(Collectors.joining("\n"));

            // (3) https://docs.aws.amazon.com/general/latest/gr/sigv4-calculate-signature.html
            byte[] kDate = hmac(("AWS4" + awsSecret).getBytes(StandardCharsets.UTF_8), isoJustDate);
            byte[] kRegion = hmac(kDate, awsRegion);
            byte[] kService = hmac(kRegion, awsService);
            byte[] kSigning = hmac(kService, "aws4_request");
            String signature = hex(hmac(kSigning, stringToSign));

            String authParameter = "AWS4-HMAC-SHA256 Credential=" + awsIdentity + "/" + credentialScope + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
            headers.put("Authorization", authParameter);

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new IllegalStateException(e);
            }
        }
    }

    private static String normalizeSpaces(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    public static String hex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes);
        return digest.digest();
    }

    public static byte[] hmac(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

}
