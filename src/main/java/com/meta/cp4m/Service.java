/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.MessageHandler;
import com.meta.cp4m.message.RequestProcessor;
import com.meta.cp4m.message.ThreadState;
import com.meta.cp4m.plugin.Plugin;
import com.meta.cp4m.routing.Route;
import com.meta.cp4m.store.ChatStore;
import io.javalin.http.Context;
import java.io.IOException;
import java.util.ArrayList;
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
  private final Plugin<T> plugin;

  private final String path;

  public Service(ChatStore<T> store, MessageHandler<T> handler, Plugin<T> plugin, String path) {
    this.handler = Objects.requireNonNull(handler);
    this.store = Objects.requireNonNull(store);
    this.plugin = Objects.requireNonNull(plugin);
    this.path = Objects.requireNonNull(path);
  }

  <IN> void handler(Context ctx, IN in, RequestProcessor<IN, T> processor) {
    List<T> messages;
    try {
      messages = processor.process(ctx, in);
    } catch (RuntimeException e) {
      LOGGER
          .atError()
          .addKeyValue("body", ctx.body())
          .addKeyValue("headers", ctx.headerMap())
          .setMessage("unable to process request")
          .setCause(e)
          .log();
      throw e;
    }
    // TODO: once we have a non-volatile store, on startup send stored but not replied to messages
    for (T m : messages) {
      ThreadState<T> thread = store.add(m);
      executorService.submit(() -> execute(thread));
    }
  }

  public String path() {
    return path;
  }

  public MessageHandler<T> messageHandler() {
    return this.handler;
  }

  public ChatStore<T> store() {
    return this.store;
  }

  public Plugin<T> plugin() {
    return this.plugin;
  }

  private void execute(ThreadState<T> thread) {
    T llmResponse;
    try {
      llmResponse = plugin.handle(thread);
    } catch (IOException e) {
      LOGGER.error("failed to communicate with LLM", e);
      return;
    }
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

  private <E> Route<E> toRoute(MessageHandler.RouteDetails<E, T> routeDetails) {
    return new Route<>(
        path,
        routeDetails.handlerType(),
        routeDetails.acceptor(),
        (ctx, in) -> handler(ctx, in, routeDetails.requestProcessor()));
  }

  List<Route<?>> routes() {
    List<MessageHandler.RouteDetails<?, T>> routeDetails = handler.routeDetails();
    List<Route<?>> routes = new ArrayList<>(routeDetails.size());
    for (MessageHandler.RouteDetails<?, T> routeDetail : routeDetails) {
      Route<?> route = toRoute(routeDetail);
      routes.add(route);
    }
    return routes;
  }
}
