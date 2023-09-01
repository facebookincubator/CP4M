/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Configuration {
  public static final ObjectMapper MAPPER =
      new ObjectMapper()
          .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .enable(DeserializationFeature.WRAP_EXCEPTIONS)
          .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
          .enable(DeserializationFeature.USE_LONG_FOR_INTS)
          .disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
          .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
}
