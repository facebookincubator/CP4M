/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.meta.cp4m.DummyWebServer.ReceivedRequest;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.webhook.whatsapp.*;
import io.javalin.http.HandlerType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.hc.client5.http.fluent.Response;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class WAMessageHandlerTest {
  static final String VALID2 =
      """
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "373128041419556",
      "changes": [
        {
          "value": {
            "messaging_product": "whatsapp",
            "metadata": {
              "display_phone_number": "16505551111",
              "phone_number_id": "123456123"
            },
            "contacts": [
              {
                "profile": {
                  "name": "Bob Bobson"
                },
                "wa_id": "12247330922"
              }
            ],
            "messages": [
              {
                "context": {
                  "from": "13137829971",
                  "id": "wamid.HBgLMrIwqjczMzA4MjQVAgarGBjcRjg3QkEwRjZGRTY5MERBMTUA"
                },
                "from": "12027330824",
                "id": "wamid.HBgLMTIwMjczMzA4MjQVAgASGBQzQUFENTkyMDIdQjk6RjNFQjQ5MbA=",
                "timestamp": "1723063165",
                "text": {
                  "body": "Coolio"
                },
                "type": "text"
              }
            ]
          },
          "field": "messages"
        }
      ]
    }
  ]
}
""";
  static final String VALID =
      """
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "0",
      "changes": [
        {
          "field": "messages",
          "value": {
            "messaging_product": "whatsapp",
            "metadata": {
              "display_phone_number": "16505551111",
              "phone_number_id": "123456123"
            },
            "contacts": [
              {
                "profile": {
                  "name": "test user name"
                },
                "wa_id": "16315551181"
              }
            ],
            "messages": [
              {
                "from": "16315551181",
                "id": "ABGGFlA5Fpa",
                "timestamp": "1504902988",
                "type": "text",
                "text": {
                  "body": \"this is a text message!@#$%^&*()’\"
                }
              }
            ]
          }
        }
      ]
    }
  ]
}
""";
  static final String NO_MESSAGES =
      """
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "0",
      "changes": [
        {
          "field": "messages",
          "value": {
            "messaging_product": "whatsapp",
            "metadata": {
              "display_phone_number": "16505551111",
              "phone_number_id": "123456123"
            },
            "contacts": [
              {
                "profile": {
                  "name": "test user name"
                },
                "wa_id": "16315551181"
              }
            ]
          }
        }
      ]
    }
  ]
}
""";
  private static final JsonMapper MAPPER = Utils.JSON_MAPPER;
  private ServiceTestHarness<WAMessage> harness = ServiceTestHarness.newWAServiceTestHarness();

  @BeforeEach
  void setUp() {
    harness = ServiceTestHarness.newWAServiceTestHarness();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"Hello Worldy!!", ""})
  void welcomeMessage(final @Nullable String welcomeMessage)
      throws IOException, InterruptedException {
    ServiceTestHarness<WAMessage> harness =
        this.harness.withHandler(
            WAMessengerConfig.of(
                    this.harness.verifyToken(),
                    this.harness.appSecret(),
                    this.harness.accessToken(),
                    welcomeMessage)
                .toMessageHandler());
    harness.start();
    ObjectNode webhookPayload = (ObjectNode) MAPPER.readTree(VALID);
    ObjectNode msg =
        (ObjectNode)
            webhookPayload
                .get("entry")
                .get(0)
                .get("changes")
                .get(0)
                .get("value")
                .get("messages")
                .get(0);
    msg.remove("text");
    msg.put("type", "request_welcome");
    harness.post(MAPPER.writeValueAsString(webhookPayload)).execute();
    @Nullable ReceivedRequest res = harness.pollWebserver(200);
    if (welcomeMessage == null) {
      assertThat(res).isNull();
    } else {
      assertThat(res).isNotNull();
      assertThat(MAPPER.readTree(res.body()))
          .isEqualTo(
              MAPPER.readTree(
                  "{\"recipient_type\":\"individual\",\"messaging_product\":\"whatsapp\",\"type\":\"text\",\"to\":\"16315551181\",\"text\":{\"body\":\""
                      + welcomeMessage
                      + "\"}}"));
    }
  }

  static Stream<String> validWAPayloads() {
    return Stream.of(VALID, VALID2);
  }

  @ParameterizedTest
  @MethodSource("validWAPayloads")
  void valid(String payload) throws IOException, InterruptedException {
    final String sendResponse =
        """
{
      "messaging_product": "whatsapp",
      "contacts": [
        {
          "input": "16315551181",
          "wa_id": "16315551181"
        }
      ],
      "messages": [
        {
          "id": "wamid.HBgLMTY1MDUwNzY1MjAVAgARGBI5QTNDQTVCM0Q0Q0Q2RTY3RTcA",
          "message_status": "accepted"
        }
      ]
    }""";

    // make sure we can parse the payload
    WebhookPayload payloadParsed = Utils.JSON_MAPPER.readValue(payload, WebhookPayload.class);

    harness.dummyWebServer().response(ctx -> ctx.body().contains("\"type\""), sendResponse);
    harness.start();
    Response request = harness.post(payload).execute();
    assertThat(request.returnResponse().getCode()).isEqualTo(200);

    Collection<ReceivedRequest> responses =
        Stream.of(harness.pollWebserver(500), harness.pollWebserver(500))
            .filter(Objects::nonNull)
            .toList();
    assertThat(responses)
        .hasSize(2)
        .extracting(ReceivedRequest::body)
        .extracting(MAPPER::readTree)
        .anyMatch(b -> b.findPath("status").asText().equals("read")) // one is a read receipt
        .anySatisfy(
            b ->
                assertThat(b.findPath("text").findPath("body").textValue())
                    .isEqualTo(harness.dummyPluginResponseText()));

    List<ThreadState<WAMessage>> threads = harness.chatStore().list();
    assertThat(threads).hasSize(1);
    ThreadState<WAMessage> thread = threads.get(0);
    Value valueParsed =
        payloadParsed.entry().stream().findAny().orElseThrow().changes().stream()
            .findAny()
            .orElseThrow()
            .value();
    TextWebhookMessage parsedMessage =
        (TextWebhookMessage) valueParsed.messages().stream().findAny().orElseThrow();
    assertThat(thread.messages())
        .hasSize(2)
        .anySatisfy(
            m -> {
              assertThat(m.message()).isEqualTo(parsedMessage.text().body());
              assertThat(m.instanceId()).isEqualTo(parsedMessage.id());
              assertThat(m.senderId()).isEqualTo(parsedMessage.from());
              assertThat(m.recipientId()).isEqualTo(valueParsed.metadata().phoneNumberId());
              assertThat(m.role()).isEqualTo(Message.Role.USER);
            })
        .anySatisfy(
            m -> {
              assertThat(m.role()).isEqualTo(Message.Role.ASSISTANT);
              assertThat(m.senderId()).isEqualTo(valueParsed.metadata().phoneNumberId());
              assertThat(m.recipientId()).isEqualTo(parsedMessage.from());
            });
    String testUserName =
        MAPPER
            .readTree(payload)
            .get("entry")
            .get(0)
            .get("changes")
            .get(0)
            .get("value")
            .get("contacts")
            .get(0)
            .get("profile")
            .get("name")
            .textValue();
    Stopwatch stopwatch = Stopwatch.createStarted();
    // sometimes we need to wait for the phone number
    while (!harness.chatStore().list().getFirst().userData().phoneNumber().isPresent()
        && stopwatch.elapsed().minusMillis(500).isNegative()) {
      Thread.sleep(10);
    }
    thread = harness.chatStore().list().getFirst();
    assertThat(thread.userData())
        .satisfies(u -> assertThat(u.name()).get().isEqualTo(testUserName))
        .satisfies(u -> assertThat(u.phoneNumber()).get().isEqualTo("16315551181"));

    // repeat and show that it is not processed again because it is a duplicate
    request = harness.post(payload).execute();
    assertThat(request.returnResponse().getCode()).isEqualTo(200);
    assertThat(harness.pollWebserver(500)).isNull();
  }

  @Test
  void validResponseWithoutContacts() throws IOException {
    final String sendResponse =
        """
{
      "messaging_product": "whatsapp",
      "contacts": [
      ],
      "messages": [
        {
          "id": "wamid.HBgLMTY1MDUwNzY1MjAVAgARGBI5QTNDQTVCM9Q0Q0Q2RTY3RTcA",
          "message_status": "accepted"
        }
      ]
    }""";
    harness.dummyWebServer().response(ctx -> ctx.body().contains("\"type\""), sendResponse);
    harness.start();
    WAMessageHandler handler = (WAMessageHandler) harness.handler();
    SendResponse response = handler.send(Identifier.random(), Identifier.random(), "test");
    assertThat(response.contacts()).isEmpty();
    assertThat(response.messages().getFirst().messageId())
        .isEqualTo("wamid.HBgLMTY1MDUwNzY1MjAVAgARGBI5QTNDQTVCM9Q0Q0Q2RTY3RTcA");
  }

  @Test
  void mediaFetchValid() throws IOException, URISyntaxException {
    final String webhookResponse =
        """
{
  "messaging_product": "whatsapp",
  "url": "https://example.com/image.jpg",
  "mime_type": "image/jpeg",
  "sha256": "f1234567890",
  "file_size": "111",
  "id": "1234567890"
}""";
    harness.dummyWebServer().response(ctx -> ctx.method().equals(HandlerType.GET), webhookResponse);
    harness.start();
    WAMessageHandler handler = (WAMessageHandler) harness.handler();
    GetMediaIdBody response = handler.mediaDetails("1234567890");
    assertThat(response.url()).isEqualTo(new URI("https://example.com/image.jpg"));
  }

  @Test
  void doesNotSendNonTextMessages() throws IOException, InterruptedException {
    harness.start();
    harness.plugin().addResponseToSend(new Payload.Image(new byte[0], "image/jpeg"));
    Response request = harness.post(VALID).execute();
    assertThat(request.returnResponse().getCode()).isEqualTo(200);
    List<ReceivedRequest> responses = new ArrayList<>(1);
    @Nullable ReceivedRequest r = harness.pollWebserver(500);
    while (r != null) {
      responses.add(r);
      r = harness.pollWebserver(500);
    }
    // only receive a read receipt, but not message
    assertThat(responses).hasSize(1);
    assertThat(MAPPER.readTree(responses.get(0).body()).findPath("status").textValue())
        .isEqualTo("read");
  }

  @Test
  void noMessages() throws IOException, InterruptedException {
    harness.start();
    Response request = harness.post(NO_MESSAGES).execute();
    assertThat(request.returnResponse().getCode()).isEqualTo(200);
    assertThat(harness.pollWebserver(250)).isNull();
    assertThat(harness.chatStore().list()).hasSize(0);
  }
}
