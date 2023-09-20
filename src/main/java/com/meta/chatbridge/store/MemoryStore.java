/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.store;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.meta.chatbridge.Identifier;
import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.message.MessageStack;
import java.time.Duration;

public class MemoryStore<T extends Message> implements ChatStore<T> {
  private final Cache<Identifier, MessageStack<T>> store;

  MemoryStore(MemoryStoreConfig config) {
    this.store =
        CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(config.storageDurationHours()))
            .maximumWeight((long) (config.storageCapacityMb() * Math.pow(2, 20))) // megabytes
            .<Identifier, MessageStack<T>>weigher(
                (k, v) ->
                    v.messages().stream().map(m -> m.message().length()).reduce(0, Integer::sum))
            .build();
  }

  @Override
  public MessageStack<T> add(T message) {
    return this.store
        .asMap()
        .compute(
            message.threadId(),
            (k, v) -> {
              if (v == null) {
                return MessageStack.of(message);
              }
              return v.with(message);
            });
  }

  public long size() {
    return store.size();
  }
}
