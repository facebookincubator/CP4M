/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.store;

import com.google.common.cache.Cache;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;

import java.util.List;

/**
 * This class is a default store that does not persist any data.
 *
 * @param <T> the type of message being passed stored
 */
public class NullStore<T extends Message> implements ChatStore<T> {

    @Override
    public ThreadState<T> add(T message) {
        return ThreadState.of(message);
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public List<ThreadState<T>> list() {
        return List.of();
    }
}