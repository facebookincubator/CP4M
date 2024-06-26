/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;

public class DummyWebServer implements AutoCloseable {

  private static final JsonMapper MAPPER = new JsonMapper();
  private final Javalin javalin;
  private final BlockingQueue<ReceivedRequest> receivedRequests = new LinkedBlockingDeque<>();
  private final List<ResponseRule> responses = new ArrayList<>();

  private DummyWebServer() {
    this.javalin = Javalin.create();
    javalin.before(
        ctx ->
            receivedRequests.put(
                new ReceivedRequest(
                    ctx.path(),
                    ctx.body(),
                    ctx.contentType(),
                    ctx.headerMap(),
                    ctx.queryParamMap())));
    Arrays.stream(HandlerType.values())
        .forEach(
            ht ->
                this.javalin.addHttpHandler(
                    ht,
                    "/<path>",
                    ctx -> {
                      for (ResponseRule rr : responses) {
                        if (rr.applies.apply(ctx)) {
                          ctx.result(rr.response.apply(ctx));
                          return;
                        }
                      }
                    }));
    javalin.start(0);
  }

  public @This DummyWebServer response(Function<Context, Boolean> applies, JsonNode body)
      throws JsonProcessingException {
    return response(applies, MAPPER.writeValueAsString(body));
  }

  public @This DummyWebServer response(Function<Context, Boolean> applies, String body) {
    responses.add(new ResponseRule(applies, ignored -> body));
    return this;
  }

  public @This DummyWebServer response(
      Function<Context, Boolean> applies, Function<Context, String> body) {
    responses.add(new ResponseRule(applies, body));
    return this;
  }

  private record ResponseRule(
      Function<Context, Boolean> applies, Function<Context, String> response) {}

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

  public List<ReceivedRequest> takeAll(long milliseconds) throws InterruptedException {
    ArrayList<ReceivedRequest> out = new ArrayList<>(receivedRequests.size());
    out.add(take(milliseconds));
    receivedRequests.drainTo(out);
    return out;
  }

  public int port() {
    return javalin.port();
  }

  public Javalin javalin() {
    return javalin;
  }

  @Override
  public void close() {
    javalin.stop();
  }

  public record ReceivedRequest(
      String path,
      String body,
      @Nullable String contentType,
      Map<String, String> headerMap,
      Map<String, java.util.List<String>> stringListMap) {}
}
