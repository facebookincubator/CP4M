/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;
import java.io.IOException;
import java.time.Instant;

public class EchoPlugin<T extends Message> implements LLMPlugin<T> {

  @Override
  public T handle(ThreadState<T> threadState) throws IOException {
    return threadState.newMessageFromBot(Instant.now(), threadState.tail().message());
  }
}
