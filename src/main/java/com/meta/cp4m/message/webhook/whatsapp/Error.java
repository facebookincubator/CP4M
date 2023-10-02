/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message.webhook.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public record Error(
    int code, String title, String message, @JsonProperty("error_data") ErrorData errorData) {
  public Error {
    Objects.requireNonNull(title);
    Objects.requireNonNull(message);
    Objects.requireNonNull(errorData);
  }

  public record ErrorData(String details) {
    public ErrorData {
      Objects.requireNonNull(details);
    }
  }
}
