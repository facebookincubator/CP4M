/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.store;

public class LLMContextManager {
    private final String context;

    public LLMContextManager(String context) {
        this.context = context;
    }

    public String getContext() {
        return context;
    }
}
