/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.store;

import com.meta.cp4m.message.Message;

public record NullStoreConfig(String name) implements StoreConfig {

  @Override
  public <T extends Message> ChatStore<T> toStore() {
    return new NullStore<>();
  }
}
