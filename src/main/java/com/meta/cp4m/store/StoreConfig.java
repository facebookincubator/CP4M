/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.store;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.meta.cp4m.message.Message;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = MemoryStoreConfig.class, name = "memory"),
  @JsonSubTypes.Type(value = NullStoreConfig.class, name = "null"),
})
public interface StoreConfig {

  String name();

  <T extends Message> ChatStore<T> toStore();
}
