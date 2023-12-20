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
import com.meta.cp4m.DummyWebServer.ReceivedRequest;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.webhook.whatsapp.Utils;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.hc.client5.http.fluent.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WAMessageHandlerTest {

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
                  "body": "this is a text message"
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
  private final ServiceTestHarness<WAMessage> harness =
      ServiceTestHarness.newWAServiceTestHarness();

  @BeforeEach
  void setUp() {
    harness.start();
  }

  @Test
  void valid() throws IOException, InterruptedException {
    Response request = harness.post(VALID).execute();
    assertThat(request.returnResponse().getCode()).isEqualTo(200);

    Collection<ReceivedRequest> responses =
        Stream.of(harness.pollWebserver(1000), harness.pollWebserver(1000))
            .filter(Objects::nonNull)
            .toList();

    assertThat(responses)
        .hasSize(2)
        .extracting(ReceivedRequest::body)
        .extracting(MAPPER::readTree)
        .anyMatch(b -> b.findPath("status").asText().equals("read"))
        .anySatisfy(
            b ->
                assertThat(b.findPath("text").findPath("body").textValue())
                    .isEqualTo(harness.dummyPluginResponseText()));

    List<ThreadState<WAMessage>> threads = harness.chatStore().list();
    assertThat(threads).hasSize(1);
    ThreadState<WAMessage> thread = threads.get(0);
    assertThat(thread.messages())
        .hasSize(2)
        .anySatisfy(
            m -> {
              assertThat(m.message()).isEqualTo("this is a text message");
              assertThat(m.instanceId()).isEqualTo(Identifier.from("ABGGFlA5Fpa"));
              assertThat(m.senderId()).isEqualTo(Identifier.from("16315551181"));
              assertThat(m.recipientId()).isEqualTo(Identifier.from("123456123"));
              assertThat(m.role()).isEqualTo(Message.Role.USER);
            })
        .anySatisfy(
            m -> {
              assertThat(m.role()).isEqualTo(Message.Role.ASSISTANT);
              assertThat(m.senderId()).isEqualTo(Identifier.from("123456123"));
              assertThat(m.recipientId()).isEqualTo(Identifier.from("16315551181"));
            });
  }

  @Test
  void noMessages() throws IOException, InterruptedException {
    Response request = harness.post(NO_MESSAGES).execute();
    assertThat(request.returnResponse().getCode()).isEqualTo(200);
    assertThat(harness.pollWebserver(250)).isNull();
    assertThat(harness.chatStore().list()).hasSize(0);
  }
}
