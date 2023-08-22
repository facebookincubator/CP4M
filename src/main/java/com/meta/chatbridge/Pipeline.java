/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge;

import com.meta.chatbridge.llm.LLMHandler;
import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.message.MessageHandler;
import com.meta.chatbridge.store.ChatStore;
import com.meta.chatbridge.store.MessageStack;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pipeline<T extends Message> {

  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final MessageHandler<T> handler;
  private final ChatStore<T> store;
  private final LLMHandler<T> llmHandler;

  private final String endpoint;

  public Pipeline(
      ChatStore<T> store, MessageHandler<T> handler, LLMHandler<T> llmHandler, String endpoint) {
    this.handler = Objects.requireNonNull(handler);
    this.store = Objects.requireNonNull(store);
    this.llmHandler = llmHandler;
    this.endpoint = endpoint;
  }

  void handle(Context ctx) {
    // TODO: handle de-duplication
    List<T> messages = handler.processRequest(ctx);
    for (T m : messages) {
      MessageStack<T> stack = store.add(m);
      executorService.submit(() -> execute(stack));
    }
  }

  public void register(Javalin app) {
    handler.handlers().forEach(m -> app.addHandler(m, endpoint, this::handle));
  }

  private void execute(MessageStack<T> stack) {
    System.out.println(stack.messages().get(stack.messages().size() - 1).message());
    T llmResponse = llmHandler.handle(stack);
    store.add(llmResponse);
    handler.respond(llmResponse);
  }
}
