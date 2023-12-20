/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DummyWebServer implements AutoCloseable {
  private final Javalin javalin;
  private final BlockingQueue<ReceivedRequest> receivedRequests = new LinkedBlockingDeque<>();

  private DummyWebServer() {
    this.javalin =
        Javalin.create()
            .addHandler(
                HandlerType.GET,
                "/<path>",
                ctx ->
                    receivedRequests.put(
                        new ReceivedRequest(
                            ctx.path(),
                            ctx.body(),
                            ctx.contentType(),
                            ctx.headerMap(),
                            ctx.queryParamMap())))
            .addHandler(
                HandlerType.POST,
                "/<path>",
                ctx ->
                    receivedRequests.put(
                        new ReceivedRequest(
                            ctx.path(),
                            ctx.body(),
                            ctx.contentType(),
                            ctx.headerMap(),
                            ctx.queryParamMap())))
            .start(0);
  }

  public static DummyWebServer create() {
    return new DummyWebServer();
  }

  public @Nullable ReceivedRequest poll() {
    return receivedRequests.poll();
  }

  public @Nullable ReceivedRequest poll(long milliseconds) throws InterruptedException {
    return receivedRequests.poll(milliseconds, TimeUnit.MILLISECONDS);
  }

  public ReceivedRequest take(long milliseconds) throws InterruptedException {
    return Objects.requireNonNull(receivedRequests.poll(milliseconds, TimeUnit.MILLISECONDS));
  }

  public int port() {
    return javalin.port();
  }

  public Javalin javalin() {
    return javalin;
  }

  @Override
  public void close() {
    javalin.close();
  }

  public record ReceivedRequest(
      String path,
      String body,
      @Nullable String contentType,
      Map<String, String> headerMap,
      Map<String, java.util.List<String>> stringListMap) {}
}
