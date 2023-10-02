/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message.webhook.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meta.cp4m.Identifier;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class WebhookPayloadTest {
  static String TEST_MESSAGE =
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

  @Test
  void validTextMessage() throws JsonProcessingException {
    WebhookPayload payload = Utils.JSON_MAPPER.readValue(TEST_MESSAGE, WebhookPayload.class);
    assertThat(payload.object()).isEqualTo("whatsapp_business_account");
    assertThat(payload.entry()).isNotEmpty();
    Collection<Change> changes = payload.entry().stream().findFirst().orElseThrow().changes();
    assertThat(changes).isNotEmpty();
    Change change = changes.stream().findFirst().orElseThrow();
    assertThat(change.field()).isEqualTo("messages");
    assertThat(change.value().messagingProduct()).isEqualTo("whatsapp");
    assertThat(change.value().metadata().displayPhoneNumber()).isEqualTo("16505551111");
    assertThat(change.value().contacts())
        .hasSize(1)
        .allSatisfy(c -> assertThat(c.waid()).isEqualTo(Identifier.from("16315551181")));
    assertThat(change.value().messages())
        .hasSize(1)
        .allSatisfy(m -> assertThat(m).isInstanceOf(TextWebhookMessage.class))
        .allSatisfy(
            m ->
                assertThat(((TextWebhookMessage) m).text().body())
                    .isEqualTo("this is a text message"));

    assertThat(change.value().errors()).isEmpty();
  }
}
