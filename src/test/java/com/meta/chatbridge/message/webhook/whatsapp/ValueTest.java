/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message.webhook.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

class ValueTest {
  static final String VALID =
      """
{
"messaging_product": "whatsapp",
"metadata": {
  "display_phone_number": "15555551234",
  "phone_number_id": "imandid"
  },
"contacts": ["""
          + ContactTest.VALID
          + """
]
}
""";
  private static final JsonMapper MAPPER = Utils.JSON_MAPPER;

  @Test
  void test() throws JsonProcessingException {
    Value value = MAPPER.readValue(VALID, Value.class);
  }
}
