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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.meta.cp4m.Identifier;
import io.javalin.http.Context;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IGMessageHandler extends FBMessageHandler{

    private final String verifyToken;
    private final String appSecret;

    private final String accessToken;
    private static final JsonMapper MAPPER = new JsonMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(IGMessageHandler.class);
    private final Deduplicator<Identifier> messageDeduplicator = new Deduplicator<>(10_000);

    public IGMessageHandler(String verifyToken, String pageAccessToken, String appSecret) {
        super(verifyToken, appSecret, pageAccessToken);
        this.verifyToken = verifyToken;
        this.appSecret = appSecret;
        this.accessToken = pageAccessToken;

    }

    IGMessageHandler(IGMessengerConfig config) {
        super(config);
        this.verifyToken = config.verifyToken();
        this.appSecret = config.appSecret();
        this.accessToken = config.pageAccessToken();

    }

    private List<FBMessage> postHandler(Context ctx) throws JsonProcessingException {
        MetaHandlerUtils.postHeaderValidator(ctx, appSecret);

        String bodyString = ctx.body();
        JsonNode body = MAPPER.readTree(bodyString);
        String object = body.get("object").textValue();
        if (!object.equals("instagram")) {
            LOGGER
                    .atWarn()
                    .setMessage("received body that has a different value for 'object' than 'instagram'")
                    .addKeyValue("body", bodyString)
                    .log();
            return Collections.emptyList();
        }
        // TODO: need better validation
        JsonNode entries = body.get("entry");
        ArrayList<FBMessage> output = new ArrayList<>();
        for (JsonNode entry : entries) {
            @Nullable JsonNode messaging = entry.get("messaging");
            if (messaging == null) {
                continue;
            }
            for (JsonNode message : messaging) {
                @Nullable JsonNode messageObject = message.get("message");

                Identifier senderId = Identifier.from(message.get("sender").get("id").asLong());
                Identifier recipientId = Identifier.from(message.get("recipient").get("id").asLong());
                Instant timestamp = Instant.ofEpochMilli(message.get("timestamp").asLong());

                if (messageObject != null) {
                    if(messageObject.get("is_echo").asText().equals("true")){
                        return Collections.emptyList();
                    }

                    // https://developers.facebook.com/docs/messenger-platform/reference/webhook-events/messages
                    Identifier messageId = Identifier.from(messageObject.get("mid").textValue());
                    if (messageDeduplicator.addAndGetIsDuplicate(messageId)) {
                        continue;
                    }

                    @Nullable JsonNode textObject = messageObject.get("text");
                    if (textObject != null && textObject.isTextual()) {
                        FBMessage m =
                                new FBMessage(
                                        timestamp,
                                        messageId,
                                        senderId,
                                        recipientId,
                                        textObject.textValue(),
                                        Message.Role.USER);
                        output.add(m);
                    } else {
                        LOGGER
                                .atWarn()
                                .setMessage("received message without text, unable to handle this")
                                .addKeyValue("body", bodyString)
                                .log();
                    }
                } else {
                    LOGGER
                            .atWarn()
                            .setMessage(
                                    "received a message without a 'message' key, unable to handle this message type")
                            .addKeyValue("body", bodyString)
                            .log();
                }
            }
        }

        return output;
    }
}
