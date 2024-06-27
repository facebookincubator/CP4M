/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.meta.cp4m.DummyWebServer;
import io.javalin.http.Context;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

class AuthRequestTest {

  private static final JsonMapper MAPPER = new JsonMapper();

  @Test
  void oauth() throws URISyntaxException, InterruptedException, IOException {
    DummyWebServer webserver = DummyWebServer.create();
    PayloadSetter payloadSetter = new PayloadSetter(1L);
    webserver.response(ctx -> ctx.path().equals("/oauth"), payloadSetter);
    AuthRequest.OauthRequest oauthRequest =
        new AuthRequest.OauthRequest(
            URIBuilder.loopbackAddress()
                .setScheme("http")
                .setPort(webserver.port())
                .appendPath("/oauth")
                .build()
                .toString(),
            "client_id",
            "client_secret",
            "audience");
    DummyWebServer.ReceivedRequest request = webserver.take(300);
    String params = URLDecoder.decode(request.body(), StandardCharsets.UTF_8);
    assertThat(params)
        .contains("client_id=client_id")
        .contains("client_secret=client_secret")
        .contains("audience=audience")
        .contains("grant_type=client_credentials");

    URI serverUrl =
        URIBuilder.loopbackAddress()
            .setScheme("http")
            .setPort(webserver.port())
            .appendPath("/server")
            .build();

    JsonNode postBody = MAPPER.readTree("{\"key\": \"value\"}");
    oauthRequest
        .post(serverUrl)
        .bodyString(MAPPER.writeValueAsString(postBody), ContentType.APPLICATION_JSON)
        .execute()
        .discardContent();

    List<DummyWebServer.ReceivedRequest> requests = webserver.takeAll(300);
    assertThat(requests)
        .filteredOn(r -> r.path().equals("/server"))
        .hasSize(1)
        .allSatisfy(r -> assertThat(r.path()).isEqualTo("/server"))
        .allSatisfy(r -> assertThat(r.body()).isEqualTo(MAPPER.writeValueAsString(postBody)))
        .allSatisfy(
            r -> assertThat(r.contentType()).isEqualTo(ContentType.APPLICATION_JSON.toString()))
        .allSatisfy(
            r -> assertThat(r.headerMap().get("Authorization")).startsWith("Bearer test_token_"));
    assertThat(webserver.take(1000).path())
        .isEqualTo("/oauth"); // make sure we're at least on the second token refresh
    oauthRequest.get(serverUrl).execute().discardContent();
    requests = webserver.takeAll(300);
    DummyWebServer.ReceivedRequest rr =
        requests.stream().filter(r -> r.path().equals("/server")).findFirst().orElseThrow();

    @Nullable String authTokenReceived = rr.headerMap().get("Authorization");
    assertThat(authTokenReceived).startsWith("Bearer test_token_");
    String tokenCount = authTokenReceived.substring(authTokenReceived.length() - 1);
    assertThat(Integer.valueOf(tokenCount)).isGreaterThan(-1);
  }

  static class PayloadSetter implements Function<Context, String> {

    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile long expiresIn;

    PayloadSetter(long expiresIn) {
      this.expiresIn = expiresIn;
    }

    @Override
    public String apply(Context context) {
      return "{\"access_token\":\"test_token_"
          + counter.getAndIncrement()
          + "\", \"scope\":\"ReadWrite\",\"expires_in\": "
          + expiresIn
          + ",\"token_type\":\"Bearer\"}";
    }
  }
}
