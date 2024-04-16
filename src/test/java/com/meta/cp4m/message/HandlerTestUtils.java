/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meta.cp4m.Identifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.net.URIBuilder;

public final class HandlerTestUtils {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // static class, do not instantiate
  private HandlerTestUtils() {}

  public static Function<Identifier, URI> baseURLFactory(String path, int port) {
    return identifier -> {
      try {
        return URIBuilder.loopbackAddress()
            .setPort(port)
            .appendPath(path)
            .setScheme("http")
            .build();
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static Function<JsonNode, Request> MessageRequestFactory(
      Method method, String path, String appSecret, int port) throws URISyntaxException {
    Request request =
        Request.create(
            method,
            URIBuilder.loopbackAddress().setScheme("http").appendPath(path).setPort(port).build());
    return jn -> {
      try {
        String body = MAPPER.writeValueAsString(jn);
        return request
            .bodyString(body, ContentType.APPLICATION_JSON)
            .setHeader("X-Hub-Signature-256", "sha256=" + MetaHandlerUtils.hmac(body, appSecret));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };
  }
}
