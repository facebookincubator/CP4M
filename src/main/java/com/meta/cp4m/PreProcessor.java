/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;

@FunctionalInterface
public interface PreProcessor<T extends Message> {

    ThreadState<T> run(ThreadState<T> in);
}