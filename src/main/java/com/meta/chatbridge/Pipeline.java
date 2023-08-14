/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.meta.chatbridge.llm.LLMHandler;
import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.message.MessageHandler;
import com.meta.chatbridge.store.ChatStore;
import com.meta.chatbridge.store.MessageStack;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pipeline<T extends Message> {

  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final MessageHandler<T> handler;
  private final ChatStore<T> store;
  private final LLMHandler<T> llmHandler;

  private Pipeline(ChatStore<T> store, MessageHandler<T> handler, LLMHandler<T> llmHandler) {
    this.handler = Objects.requireNonNull(handler);
    this.store = Objects.requireNonNull(store);
    this.llmHandler = llmHandler;
  }

  public void handle(JsonNode node) {
    T message = handler.processRequest(node);
    store.add(message);
    executorService.submit(this::execute);
  }

  private void execute() {
    // TODO: consider failure conditions
    MessageStack<T> stack = store.get();
    T llmResponse = llmHandler.handle(stack);
    store.add(llmResponse);
    handler.respond(llmResponse);
  }
}
