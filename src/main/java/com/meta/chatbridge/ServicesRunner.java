/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge;

import com.google.common.base.Preconditions;
import io.javalin.Javalin;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.returnsreceiver.qual.This;

public class ServicesRunner implements AutoCloseable {
  private final Javalin app = Javalin.create();
  private final Set<Service<?>> services = new HashSet<>();
  private boolean started = false;
  private int port = 8080;

  private ServicesRunner() {}

  public static ServicesRunner newInstance() {
    return new ServicesRunner();
  }

  public @This ServicesRunner start() {
    if (!started) {
      started = true;
      app.start(port);
    }
    return this;
  }

  public @This ServicesRunner service(Service<?> service) {
    Preconditions.checkState(!started, "cannot add service, server already started");
    if (services.add(service)) {
      service.register(app);
    }
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
