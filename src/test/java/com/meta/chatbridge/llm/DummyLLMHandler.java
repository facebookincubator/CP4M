/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.store.MessageStack;

public class DummyLLMHandler<T extends Message> implements LLMHandler<T> {

  private final T dummyLLMResponse;

  public DummyLLMHandler(T dummyLLMResponse) {
    this.dummyLLMResponse = dummyLLMResponse;
  }

  @Override
  public T handle(MessageStack<T> messageStack) {
    return dummyLLMResponse;
  }
}
