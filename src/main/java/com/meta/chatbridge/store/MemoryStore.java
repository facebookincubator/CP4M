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
import com.meta.chatbridge.FBID;
import com.meta.chatbridge.message.Message;
import java.time.Duration;

public class MemoryStore<T extends Message> implements ChatStore<T> {
  private final Cache<FBID, MessageStack<T>> store;

  public MemoryStore() {
    this.store =
        CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofDays(3))
            .maximumWeight((long) (10 * Math.pow(2, 20))) // 10 megabytes
            .<FBID, MessageStack<T>>weigher(
                (k, v) ->
                    v.messages().stream().map(m -> m.message().length()).reduce(0, Integer::sum))
            .build();
  }

  @Override
  public synchronized MessageStack<T> add(T message) {
    return this.store
        .asMap()
        .compute(
            message.conversationId(),
            (k, v) -> {
              if (v == null) {
                return MessageStack.of(message);
              }
              return v.with(message);
            });
  }
}
