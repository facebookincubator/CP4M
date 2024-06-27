/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;
import java.io.IOException;

public interface Plugin<T extends Message> {

  T handle(ThreadState<T> threadState) throws IOException;
}
