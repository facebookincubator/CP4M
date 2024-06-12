/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.llm;

import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.Payload;
import com.meta.cp4m.message.ThreadState;
import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;

public class DummyLLMPlugin<T extends Message> implements LLMPlugin<T> {

  private final String dummyLLMResponse;
  private final BlockingQueue<ThreadState<T>> receivedThreadStates = new LinkedBlockingDeque<>();
  private final Queue<Payload<?>> responsesToSend = new ConcurrentLinkedDeque<>();

  public DummyLLMPlugin(String dummyLLMResponse) {
    this.dummyLLMResponse = dummyLLMResponse;
  }

  public DummyLLMPlugin(List<Payload<?>> responses, String defaultDummyLLMResponse) {
    this.dummyLLMResponse = defaultDummyLLMResponse;
    responsesToSend.addAll(responses);
  }

  public ThreadState<T> take(int waitMs) throws InterruptedException {
    @Nullable ThreadState<T> value = receivedThreadStates.poll(waitMs, TimeUnit.MILLISECONDS);
    if (value == null) {
      throw new RuntimeException("unable to remove item from queue in under " + waitMs + "ms");
    }
    return value;
  }

  public @This DummyLLMPlugin<T> addResponseToSend(Payload<?> payload) {
    responsesToSend.add(payload);
    return this;
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
    @Nullable Payload<?> response = responsesToSend.poll();
    if (response == null) {
      return threadState.newMessageFromBot(Instant.now(), new Payload.Text(dummyLLMResponse));
    }
    return threadState.newMessageFromBot(Instant.now(), response);
  }
}
