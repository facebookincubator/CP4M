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

public class PipelinesRunner implements AutoCloseable {
  private final Javalin app = Javalin.create();
  private boolean started = false;
  private int port = 8080;

  private final Set<Pipeline<?>> pipelines = new HashSet<>();

  private PipelinesRunner() {}

  public static PipelinesRunner newInstance() {
    return new PipelinesRunner();
  }

  @This
  public PipelinesRunner start() {
    if (!started) {
      started = true;
      app.start(port);
    }
    return this;
  }

  @This
  public PipelinesRunner pipeline(Pipeline<?> pipeline) {
    Preconditions.checkState(!started, "cannot add pipeline, server already started");
    if (pipelines.add(pipeline)) {
      pipeline.register(app);
    }
    return this;
  }

  public Collection<Pipeline<?>> pipelines() {
    return Collections.unmodifiableCollection(pipelines);
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
  @This
  public PipelinesRunner port(int port) {
    Preconditions.checkState(!started, "cannot change port, server already started");
    this.port = port;
    return this;
  }

  @Override
  public void close() {
    app.close();
  }
}
