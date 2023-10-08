/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.Service;
import com.meta.cp4m.ServicesRunner;
import com.meta.cp4m.configuration.ConfigurationUtils;
import com.meta.cp4m.message.*;
import com.meta.cp4m.store.ChatStore;
import com.meta.cp4m.store.MemoryStoreConfig;
import io.javalin.Javalin;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.assertj.core.api.Assert;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class HuggingFaceLlamaIntegrationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final ArrayNode SAMPLE_RESPONSE = MAPPER.createArrayNode();
    private static final String PATH = "/";
    private static final String TEST_MESSAGE = "this is a test message";
    private static final String TEST_SYSTEM_MESSAGE = "this is a system message";
    private static final String TEST_PAYLOAD = "<s>[INST] test message [/INST]";
    private static final String TEST_PAYLOAD_WITH_SYSTEM =
            "<s>[INST] <<SYS>>\nthis is a system message\n<</SYS>>\n\nthis is a test message [/INST]";

    private static final ThreadState<FBMessage> STACK =
            ThreadState.of(
                    MessageFactory.instance(FBMessage.class)
                            .newMessage(
                                    Instant.now(),
                                    "test message",
                                    Identifier.random(),
                                    Identifier.random(),
                                    Identifier.random(),
                                    Message.Role.USER));

    static {
        SAMPLE_RESPONSE.addObject().put("generated_text", TEST_MESSAGE);
    }

    //    private BlockingQueue<HuggingFaceLlamaIntegrationTest.OutboundRequest> HuggingFaceLlamaRequests;
    private Javalin app;
    private ObjectNode minimalConfig;
    private String testMessage = "This is an integration test message";
    private String systemMessage = "You are a repeat back bot, when a user sends you a message you should respond with the exact same message the user sends you";
    private String endpoint = "https://api-inference.huggingface.co/models/meta-llama/Llama-2-13b-chat-hf";
    private String token = System.getenv("hf_access_key");
    private int maxRetries = 3; // You can change this to the desired number of retries
    private int retryDelaySeconds = 60; // Delay in seconds before retrying

    @BeforeEach
    void setUp() {
//        HuggingFaceLlamaRequests = new LinkedBlockingDeque<>();
//        app = Javalin.create();
//        app.before(
//                PATH,
//                ctx ->
//                        HuggingFaceLlamaRequests.add(
//                                new HuggingFaceLlamaIntegrationTest.OutboundRequest(ctx.body(), ctx.headerMap(), ctx.queryParamMap())));
//        app.post(PATH, ctx -> ctx.result(MAPPER.writeValueAsString(SAMPLE_RESPONSE)));
//        app.start(0);
//        endpoint =
//                URIBuilder.localhost().setScheme("http").appendPath(PATH).setPort(app.port()).build();
    }

    @Test
    void integrationTest() throws IOException, InterruptedException {
//        set endpoint
//        use access token from env
//        set system prompt
//        Create message stack
//        Send to HF URL and check response generated text
//        [OPTIONAL] send second message?
//        Messenger integration test? - just get the 200 response?
        ThreadState<FBMessage> thread =
                ThreadState.of(
                        MessageFactory.instance(FBMessage.class)
                                .newMessage(
                                        Instant.now(),
                                        systemMessage,
                                        Identifier.random(),
                                        Identifier.random(),
                                        Identifier.random(),
                                        Message.Role.SYSTEM));
        thread = thread.with(thread.newMessageFromUser(Instant.now(), testMessage, Identifier.from(2)));

        ObjectMapper MAPPER = new ObjectMapper();
        ObjectNode hfConfig = MAPPER.createObjectNode();
        hfConfig.set("api_key", TextNode.valueOf(token));
        hfConfig.set("endpoint", TextNode.valueOf(endpoint));
        hfConfig.set("name", TextNode.valueOf("Integration Test"));
        hfConfig.set("type", TextNode.valueOf("hugging_face"));
        hfConfig.set("token_limit", LongNode.valueOf(1000));
        hfConfig.set("max_output_tokens", LongNode.valueOf(256));

        HuggingFaceConfig config =
                ConfigurationUtils.jsonMapper().convertValue(hfConfig, HuggingFaceConfig.class);

        HuggingFaceLlamaPlugin<FBMessage> plugin = new HuggingFaceLlamaPlugin<FBMessage>(config);

        FBMessage message = null;

        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                message = plugin.handle(thread);; // Call your sendRequest function here

                // If the request was successful, break out of the loop
                break;
            } catch (HttpResponseException e) {
                // Handle the exception (e.g., log it)
                System.err.println("Request failed with HttpResponseException: " + e.getMessage());

                if (retryCount < maxRetries - 1) {
                    // If this is not the last retry, wait before trying again
                    System.out.println("Retrying in " + retryDelaySeconds + " seconds...");
                    try {
                        Thread.sleep(retryDelaySeconds * 1000); // Convert to milliseconds
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt(); // Restore interrupted status
                    }
                } else {
                    // Last Retry
                    assertThat(message).isNotNull();
                }
            }
        }

//        FBMessage message = plugin.handle(thread);

        assertThat(message).isNotNull();
        assertThat(message.message()).contains(testMessage);

//        System.out.println(message.message());
    }
}
