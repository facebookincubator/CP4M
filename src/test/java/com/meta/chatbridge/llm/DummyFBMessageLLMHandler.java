/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.meta.chatbridge.Identifier;
import com.meta.chatbridge.message.FBMessage;
import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.message.ThreadState;
import java.time.Instant;
import java.util.concurrent.*;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DummyFBMessageLLMHandler implements LLMPlugin<FBMessage> {

  private final String dummyLLMResponse;
  private final BlockingQueue<ThreadState<FBMessage>> receivedThreadStates =
      new LinkedBlockingDeque<>();

  public DummyFBMessageLLMHandler(String dummyLLMResponse) {
    this.dummyLLMResponse = dummyLLMResponse;
  }

  public ThreadState<FBMessage> take(int waitMs) throws InterruptedException {
    @Nullable ThreadState<FBMessage> value =
        receivedThreadStates.poll(waitMs, TimeUnit.MILLISECONDS);
    if (value == null) {
      throw new RuntimeException("unable to remove item form queue in under " + waitMs + "ms");
    }
    return value;
  }

  public ThreadState<FBMessage> take() throws InterruptedException {
    return receivedThreadStates.take();
  }

  public @Nullable ThreadState<FBMessage> poll() {
    return receivedThreadStates.poll();
  }

  public String dummyResponse() {
    return dummyLLMResponse;
  }

  @Override
  public FBMessage handle(ThreadState<FBMessage> threadState) {
    receivedThreadStates.add(threadState);
    FBMessage inbound =
        threadState.messages().stream()
            .filter(m -> m.role() == Message.Role.USER)
            .findAny()
            .orElseThrow();
    return new FBMessage(
        Instant.now(),
        Identifier.from("test_message"),
        inbound.recipientId(),
        inbound.senderId(),
        dummyLLMResponse,
        Message.Role.ASSISTANT);
  }
}
