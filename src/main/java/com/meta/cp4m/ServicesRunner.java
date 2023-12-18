/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import com.google.common.base.Preconditions;
import com.meta.cp4m.routing.Route;
import io.javalin.Javalin;

import java.util.*;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicesRunner implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServicesRunner.class);
  private final Javalin app = Javalin.create();
  private final Set<Service<?>> services = new LinkedHashSet<>();
  private boolean started = false;
  private int port = 8080;

  private ServicesRunner() {}

  public static ServicesRunner newInstance() {
    return new ServicesRunner();
  }

  private <T> boolean didAcceptAndHandle(Context ctx, Route<T> route) {
    Optional<T> acceptorOutput = route.acceptor().accept(ctx);
    if (acceptorOutput.isPresent()) {
      try {
        route.handler().handle(ctx, acceptorOutput.get());
      } catch (Exception e) {
        throw new BadRequestResponse("Unable to process request");
      }
      return true;
    }
    return false;
  }

  /**
   * Find the first route that will accept this payload and then handle the payload
   *
   * @param ctx context from Javalin
   * @param routes the routes to check for acceptability and process if accepted
   */
  private void routeSelectorAndHandler(Context ctx, List<Route<?>> routes) {
    for (Route<?> route : routes) {
      if (didAcceptAndHandle(ctx, route)) {
        return;
      }
    }
    LOGGER
        .atError()
        .setMessage("Unable to handle incoming webhook")
        .addKeyValue("body", ctx.body())
        .addKeyValue("headers", ctx.headerMap())
        .log();
    throw new BadRequestResponse("unable to handle webhook");
  }

  public @This ServicesRunner start() {
    record RouteGroup(String path, HandlerType handlerType) {}
    Map<RouteGroup, List<Route<?>>> routeGroups = new HashMap<>();
    for (Service<?> s : services) { // this is not a stream because order matters here
      s.routes()
          .forEach(
              r ->
                  routeGroups
                      .computeIfAbsent(
                          new RouteGroup(r.path(), r.handlerType()), k -> new ArrayList<>())
                      .add(r));
    }
    routeGroups.forEach(
        (routeGroup, routes) ->
            app.addHandler(
                routeGroup.handlerType(),
                routeGroup.path(),
                ctx -> this.routeSelectorAndHandler(ctx, routes)));

    if (!started) {
      started = true;
      app.start(port);
    }
    return this;
  }

  public @This ServicesRunner service(Service<?> service) {
    Preconditions.checkState(!started, "cannot add service, server already started");

    services.add(service);
    return this;
  }

  public Collection<Service<?>> services() {
    return Collections.unmodifiableCollection(services);
  }

  public int port() {
    if (started) {
      return app.port();
    }
    return port;
  }

  /**
   * Set the port that the server will start on. 0 means first available port
   *
   * @param port the port the server will start on
   * @return this
   */
  public @This ServicesRunner port(int port) {
    Preconditions.checkState(!started, "cannot change port, server already started");
    this.port = port;
    return this;
  }

  @Override
  public void close() {
    app.close();
  }
}
