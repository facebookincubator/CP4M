/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.meta.cp4m.message.webhook.whatsapp.Utils;
import com.meta.cp4m.utils.BlockingExpiringValue;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AuthRequest.NoAuthRequest.class, name = "none"),
  @JsonSubTypes.Type(value = AuthRequest.BearerTokenRequest.class, name = "bearer"),
  @JsonSubTypes.Type(value = AuthRequest.OauthRequest.class, name = "oauth2"),
})
public interface AuthRequest {

  Request post(URI url);

  Request get(URI url);

  class NoAuthRequest implements AuthRequest {

    @JsonCreator
    public NoAuthRequest() {}

    @Override
    public Request post(URI url) {
      return Request.post(url);
    }

    @Override
    public Request get(URI url) {
      return Request.get(url);
    }
  }

  class BearerTokenRequest implements AuthRequest {
    private final String token;

    @JsonCreator
    public BearerTokenRequest(@JsonProperty("access_token") String token) {
      this.token = token;
    }

    @Override
    public Request post(URI url) {
      return Request.post(url).addHeader("Authorization", "Bearer " + token);
    }

    @Override
    public Request get(URI url) {
      return Request.get(url).addHeader("Authorization", "Bearer " + token);
    }
  }

  class OauthRequest implements AuthRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthRequest.class);
    private static final JsonMapper MAPPER = Utils.JSON_MAPPER;
    private final ScheduledExecutorService scheduledExecutor =
        Executors.newSingleThreadScheduledExecutor();
    private final URI oauthTenantUrl;
    private final String clientId;
    private final String clientSecret;

    private final @Nullable String audience;

    private final Collection<BasicNameValuePair> refreshParams;
    private final BlockingExpiringValue<String> token =
        new BlockingExpiringValue<>(Instant.EPOCH, "");

    @JsonCreator
    public OauthRequest(
        @JsonProperty("tenant_url") String oauthTenantUrl,
        @JsonProperty("client_id") String clientId,
        @JsonProperty("client_secret") String clientSecret,
        @JsonProperty("audience") @Nullable String audience) {
      this.oauthTenantUrl = URI.create(Objects.requireNonNull(oauthTenantUrl));
      this.clientId = Objects.requireNonNull(clientId);
      this.clientSecret = Objects.requireNonNull(clientSecret);
      this.audience = audience;

      ImmutableList.Builder<BasicNameValuePair> refreshParams =
          ImmutableList.<BasicNameValuePair>builder()
              .add(new BasicNameValuePair("grant_type", "client_credentials"))
              .add(new BasicNameValuePair("client_id", this.clientId))
              .add(new BasicNameValuePair("client_secret", this.clientSecret));
      if (audience != null) {
        refreshParams.add(new BasicNameValuePair("audience", this.audience));
      }
      this.refreshParams = refreshParams.build();
      scheduleRefresh(0);
    }

    private OauthRefreshResponse refreshToken() throws IOException {
      return Request.post(oauthTenantUrl)
          .bodyForm(refreshParams, StandardCharsets.UTF_8)
          .execute()
          .handleResponse(
              r -> {
                String body = new String(r.getEntity().getContent().readAllBytes());
                try {
                  return MAPPER.readValue(body, OauthRefreshResponse.class);
                } catch (Exception e) {
                  LOGGER
                      .atError()
                      .setCause(e)
                      .setMessage("failed to parse OAuth response")
                      .addKeyValue("response_body", body)
                      .log();
                  throw new RuntimeException(e);
                }
              });
    }

    private void scheduleRefresh(long timeSeconds) {
      scheduledExecutor.schedule(
          () -> {
            @Nullable OauthRefreshResponse res = null;
            try {
              res = refreshToken();
              token.update(res.expiresAt().orElse(Instant.MAX), res.accessToken());
            } catch (IOException e) {
              LOGGER.atError().setCause(e).setMessage("failed to updated OAuth token").log();
              throw new RuntimeException(e);
            } finally {
              if (res == null) {
                // failed to get new token, wait a few second to try again to avoid spamming
                scheduleRefresh(3);
              } else if (res.expiresAt().isPresent()) {
                // 5 minutes before expiration or at the halfway point between now and expiration,
                // whichever is longer
                long secondToExpiration =
                    Long.max(0, Instant.now().until(res.expiresAt().get(), ChronoUnit.SECONDS));
                scheduleRefresh(Long.max(secondToExpiration - 5 * 60, secondToExpiration / 2));
              }
            }
          },
          timeSeconds,
          TimeUnit.SECONDS);
    }

    @Override
    public Request post(URI url) {
      try {
        return Request.post(url).addHeader("Authorization", "Bearer " + token.get());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Request get(URI url) {
      try {
        return Request.get(url).addHeader("Authorization", "Bearer " + token.get());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    static final class OauthRefreshResponse {
      private final String accessToken;
      private final String tokenType;
      private final @Nullable Long expiresInSeconds;
      private final @Nullable String scope;
      private final @Nullable Instant expiresAt;

      private final @Nullable String refreshToken;

      @JsonCreator
      OauthRefreshResponse(
          @JsonProperty("access_token") String accessToken,
          @JsonProperty("token_type") String tokenType,
          @JsonProperty("expires_in") @Nullable Long expiresInSeconds,
          @JsonProperty("scope") @Nullable String scope,
          @JsonProperty("refresh_token") @Nullable String refreshToken) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresInSeconds = expiresInSeconds;
        this.scope = scope;
        this.expiresAt =
            expiresInSeconds == null ? null : Instant.now().plusSeconds(expiresInSeconds);
        this.refreshToken = refreshToken;
      }

      public String accessToken() {
        return accessToken;
      }

      public String tokenType() {
        return tokenType;
      }

      public OptionalLong expiresInSeconds() {
        return expiresInSeconds == null ? OptionalLong.empty() : OptionalLong.of(expiresInSeconds);
      }

      public Optional<String> scope() {
        return Optional.ofNullable(scope);
      }

      public Optional<Instant> expiresAt() {
        return Optional.ofNullable(expiresAt);
      }

      public Optional<String> refreshToken() {
        return Optional.ofNullable(refreshToken);
      }
    }
  }
}
