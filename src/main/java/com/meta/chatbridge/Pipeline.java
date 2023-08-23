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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pipeline<T extends Message> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Pipeline.class);
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final MessageHandler<T> handler;
  private final ChatStore<T> store;
  private final LLMHandler<T> llmHandler;

  private final String path;

  public Pipeline(
      ChatStore<T> store, MessageHandler<T> handler, LLMHandler<T> llmHandler, String path) {
    this.handler = Objects.requireNonNull(handler);
    this.store = Objects.requireNonNull(store);
    this.llmHandler = llmHandler;
    this.path = path;
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
    handler.handlers().forEach(m -> app.addHandler(m, path, this::handle));
  }

  public String path() {
    return path;
  }

  private void execute(MessageStack<T> stack) {
    System.out.println(stack.messages().get(stack.messages().size() - 1).message());
    T llmResponse = llmHandler.handle(stack);
    store.add(llmResponse);
    try {
      handler.respond(llmResponse);
    } catch (Exception e) {
      LOGGER.error("failed to respond to user", e);
      // TODO: create transactional store add
      // TODO: retry
    }
  }
}
