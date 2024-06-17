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
import java.time.Instant;
import java.util.concurrent.*;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DummyPlugin<T extends Message> implements Plugin<T> {

  private final String dummyLLMResponse;
  private final BlockingQueue<ThreadState<T>> receivedThreadStates = new LinkedBlockingDeque<>();

  public DummyPlugin(String dummyLLMResponse) {
    this.dummyLLMResponse = dummyLLMResponse;
  }

  public ThreadState<T> take(int waitMs) throws InterruptedException {
    @Nullable ThreadState<T> value = receivedThreadStates.poll(waitMs, TimeUnit.MILLISECONDS);
    if (value == null) {
      throw new RuntimeException("unable to remove item from queue in under " + waitMs + "ms");
    }
    return value;
  }

  public ThreadState<T> take() throws InterruptedException {
    return receivedThreadStates.take();
  }

  public @Nullable ThreadState<T> poll() {
    return receivedThreadStates.poll();
  }

  public String dummyResponse() {
    return dummyLLMResponse;
  }

  @Override
  public T handle(ThreadState<T> threadState) {
    receivedThreadStates.add(threadState);
    return threadState.newMessageFromBot(Instant.now(), dummyLLMResponse);
  }
}
