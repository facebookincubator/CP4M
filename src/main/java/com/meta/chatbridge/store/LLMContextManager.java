/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.store;

public class LLMContextManager {
    private static String context = "";

    public static void setContext(String newContext) {
        context = newContext;
    }

    public static String getContext() {
        return context;
    }
}
