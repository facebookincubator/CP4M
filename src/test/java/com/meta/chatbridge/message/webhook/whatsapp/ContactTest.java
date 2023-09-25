/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message.webhook.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.meta.chatbridge.Identifier;
import org.junit.jupiter.api.Test;

class ContactTest {
  static final String VALID = "{\"profile\": {\"name\": \"test\"}, \"waid\": \"test\"}";
  private static final JsonMapper MAPPER = Utils.JSON_MAPPER;

  @Test
  void test() throws JsonProcessingException {
    assertThrows(ValueInstantiationException.class, () -> MAPPER.readValue("{}", Contact.class));
    assertThrows(
        ValueInstantiationException.class,
        () -> MAPPER.readValue("{\"waid\": \"test\"}", Contact.class));
    assertThrows(
        ValueInstantiationException.class,
        () -> MAPPER.readValue("{\"profile\": {\"name\": \"test\"}}", Contact.class));

    Contact contact = MAPPER.readValue(VALID, Contact.class);
    assertThat(contact.profile().name()).isEqualTo("test");
    assertThat(contact.waid()).isEqualTo(Identifier.from("test"));
  }
}
