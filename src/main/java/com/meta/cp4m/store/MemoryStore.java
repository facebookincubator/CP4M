/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.store;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;
import java.time.Duration;
import java.util.List;

public class MemoryStore<T extends Message> implements ChatStore<T> {
  private final Cache<Identifier, ThreadState<T>> store;

  MemoryStore(MemoryStoreConfig config) {
    this.store =
        CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(config.storageDurationHours()))
            .maximumWeight((long) (config.storageCapacityMb() * Math.pow(2, 20))) // megabytes
            .<Identifier, ThreadState<T>>weigher(
                (k, v) ->
                    v.messages().stream().map(m -> m.message().length()).reduce(0, Integer::sum))
            .build();
  }

  @Override
  public ThreadState<T>  add(T message) {
    return this.store
        .asMap()
        .compute(
            message.threadId(),
            (k, v) -> {
              if (v == null) {
                return ThreadState.of(message);
              }
              return v.with(message);
            });
  }

  @Override
  public long size() {
    return store.size();
  }

  @Override
  public List<ThreadState<T>> list() {
    return store.asMap().values().stream().toList();
  }

  @Override
  public ThreadState<T> get(Identifier threadId){
    return this.store.asMap().get(threadId);
  }
}
