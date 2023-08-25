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
import com.meta.chatbridge.store.MessageStack;
import java.time.Instant;
import java.util.concurrent.*;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DummyFBMessageLLMHandler implements LLMHandler<FBMessage> {

  private final String dummyLLMResponse;
  private final BlockingQueue<MessageStack<FBMessage>> receivedMessageStacks =
      new LinkedBlockingDeque<>();

  public DummyFBMessageLLMHandler(String dummyLLMResponse) {
    this.dummyLLMResponse = dummyLLMResponse;
  }

  public MessageStack<FBMessage> take(int waitMs) throws InterruptedException {
    @Nullable MessageStack<FBMessage> value =
        receivedMessageStacks.poll(waitMs, TimeUnit.MILLISECONDS);
    if (value == null) {
      throw new RuntimeException("unable to remove item form queue in under " + waitMs + "ms");
    }
    return value;
  }

  public MessageStack<FBMessage> take() throws InterruptedException {
    return receivedMessageStacks.take();
  }

  public @Nullable MessageStack<FBMessage> poll() {
    return receivedMessageStacks.poll();
  }

  public String dummyResponse() {
    return dummyLLMResponse;
  }

  @Override
  public FBMessage handle(MessageStack<FBMessage> messageStack) {
    receivedMessageStacks.add(messageStack);
    FBMessage inbound =
        messageStack.messages().stream().filter(m -> m.role() == Message.Role.USER).findAny().get();
    return new FBMessage(
        Instant.now(),
        Identifier.from("test_message"),
        inbound.recipientId(),
        inbound.senderId(),
        dummyLLMResponse,
        Message.Role.ASSISTANT);
  }
}
