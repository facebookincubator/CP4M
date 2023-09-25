/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.meta.chatbridge.Identifier;
import com.meta.chatbridge.message.webhook.whatsapp.Utils;
import com.meta.chatbridge.message.webhook.whatsapp.WebhookPayload;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WAMessageHandler implements MessageHandler<WAMessage> {
  private static final String API_VERSION = "v17.0";
  private static final JsonMapper MAPPER = Utils.JSON_MAPPER;
  private static final Logger LOGGER = LoggerFactory.getLogger(FBMessageHandler.class);

  private final Deduplicator<Identifier> messageDeduplicator = new Deduplicator<>(10_000);
  private final String appSecret;
  private final String verifyToken;

  public WAMessageHandler(String verifyToken, String appSecret) {
    this.verifyToken = verifyToken;
    this.appSecret = appSecret;
  }

  @Override
  public List<WAMessage> processRequest(Context ctx) {

    try {
      switch (ctx.handlerType()) {
        case GET -> {
          MetaHandlerUtils.subscriptionVerification(ctx, verifyToken);
          LOGGER.debug("Meta verified callback url successfully");
          return Collections.emptyList();
        }
        case POST -> {
          return postHandler(ctx);
        }
      }
    } catch (RuntimeException e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
    throw new UnsupportedOperationException("Only accepting get and post methods");
  }

  List<WAMessage> postHandler(Context ctx) {
    MetaHandlerUtils.postHeaderValidator(ctx, appSecret);
    String bodyString = ctx.body();
    WebhookPayload payload;
    try {
      payload = MAPPER.readValue(bodyString, WebhookPayload.class);
    } catch (Exception e) {
      LOGGER
          .atWarn()
          .addKeyValue("body", bodyString)
          .setMessage(
              "unable to process message, server may be subscribed to a 'webhook field' it cannot process")
          .log();
      throw new RuntimeException(e);
    }

    return null;
  }

  @Override
  public void respond(WAMessage message) throws IOException {}

  @Override
  public Collection<HandlerType> handlers() {
    return List.of(HandlerType.GET, HandlerType.POST);
  }
}
