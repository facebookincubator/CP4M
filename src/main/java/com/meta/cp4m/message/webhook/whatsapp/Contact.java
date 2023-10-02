/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message.webhook.whatsapp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meta.cp4m.Identifier;
import java.util.Objects;

public record Contact(Identifier waid, Profile profile) {

  @JsonCreator
  public Contact(@JsonProperty("wa_id") String waid, @JsonProperty("profile") Profile profile) {
    this(Identifier.from(Objects.requireNonNull(waid)), Objects.requireNonNull(profile));
  }

  public record Profile(String name) {
    public Profile {
      Objects.requireNonNull(name);
    }
  }
}
