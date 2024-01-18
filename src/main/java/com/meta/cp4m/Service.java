/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import com.meta.cp4m.llm.LLMPlugin;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.MessageHandler;
import com.meta.cp4m.message.ThreadState;
import com.meta.cp4m.store.ChatStore;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Service<T extends Message> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final MessageHandler<T> handler;
  private final ChatStore<T> store;
  private final LLMPlugin<T> llmPlugin;

  private final String path;

  public Service(
      ChatStore<T> store, MessageHandler<T> handler, LLMPlugin<T> llmPlugin, String path) {
    this.handler = Objects.requireNonNull(handler);
    this.store = Objects.requireNonNull(store);
    this.llmPlugin = llmPlugin;
    this.path = path;
  }

  void handle(Context ctx) {
    List<T> messages = handler.processRequest(ctx);
    // TODO: once we have a non-volatile store, on startup send stored but not replied to messages
    for (T m : messages) {
      ThreadState<T> thread = store.add(m);
      executorService.submit(() -> execute(thread));
    }
  }

  public void register(Javalin app) {
    handler.handlers().forEach(m -> app.addHandler(m, path, this::handle));
  }

  public String path() {
    return path;
  }

  public MessageHandler<T> messageHandler() {
    return this.handler;
  }

  private void execute(ThreadState<T> thread) {
    T llmResponse;
    try {
      llmResponse = llmPlugin.handle(thread);
    } catch (IOException e) {
      LOGGER.error("failed to communicate with LLM", e);
      return;
    }
    llmResponse = thread.tail();
    store.add(llmResponse);
    try {
      handler.respond(llmResponse);
    } catch (Exception e) {
      // we log in the handler where we have the body context
      // TODO: create transactional store add
      // TODO: implement retry with exponential backoff
      LOGGER.error("an error occurred while attempting to respond", e);
    }
  }
}
