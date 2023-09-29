/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.meta.chatbridge.Service;
import com.meta.chatbridge.ServicesRunner;
import com.meta.chatbridge.llm.DummyLLMPlugin;
import com.meta.chatbridge.llm.LLMPlugin;
import com.meta.chatbridge.store.ChatStore;
import com.meta.chatbridge.store.MemoryStoreConfig;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.reflection.qual.NewInstance;
import org.checkerframework.common.returnsreceiver.qual.This;

public class ServiceTestHarness<T extends Message> {
  private static final String VERIFY_TOKEN = "test_verify_token";
  private static final String ACCESS_TOKEN = "test_access_token";
  private static final String APP_SECRET = "test_app_secret";
  private static final String SERVICE_PATH = "/testservice";
  private static final String WEBSERVER_PATH = "/testserver";
  private final BlockingQueue<ReceivedRequest> receivedRequests = new ArrayBlockingQueue<>(1000);
  private final ChatStore<T> chatStore;
  private final MessageHandler<T> handler;
  private final LLMPlugin<T> llmPlugin;
  private final Service<T> service;
  private final ServicesRunner runner;
  private final Javalin javalin;

  private ServiceTestHarness(
      ChatStore<T> chatStore, MessageHandler<T> handler, LLMPlugin<T> llmPlugin) {
    this.chatStore = chatStore;
    this.handler = handler;
    this.llmPlugin = llmPlugin;
    this.service = new Service<>(chatStore, handler, llmPlugin, SERVICE_PATH);
    this.runner = ServicesRunner.newInstance().service(service);
    javalin = newJavalin();
  }

  public static ServiceTestHarness<WAMessage> newWAServiceTestHarness() {
    ChatStore<WAMessage> chatStore = MemoryStoreConfig.of(1, 1).toStore();
    DummyLLMPlugin<WAMessage> llmPlugin = new DummyLLMPlugin<>("dummy plugin response text");
    WAMessageHandler handler =
        WAMessengerConfig.of(VERIFY_TOKEN, APP_SECRET, ACCESS_TOKEN).toMessageHandler();
    ServiceTestHarness<WAMessage> harness = new ServiceTestHarness<>(chatStore, handler, llmPlugin);
    handler.baseUrlFactory(ignored -> harness.webserverURI());
    return harness;
  }

  public Service<T> service() {
    return service;
  }

  public ServicesRunner runner() {
    return runner;
  }

  public Javalin javalin() {
    return javalin;
  }

  private Javalin newJavalin() {
    Javalin javalin = Javalin.create();
    javalin
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
                        ctx.queryParamMap())));
    return javalin;
  }

  public Request post() {
    return Request.post(serviceURI());
  }

  public Request post(String body, boolean calculateHmac) {
    Request post = post().bodyString(body, ContentType.APPLICATION_JSON);
    if (calculateHmac) {
      post.setHeader("X-Hub-Signature-256", "sha256=" + MetaHandlerUtils.hmac(body, appSecret()));
    }
    return post;
  }

  public Request post(String body) {
    return post(body, true);
  }

  public URI webserverURI() {
    try {
      return URIBuilder.localhost()
          .appendPath(WEBSERVER_PATH)
          .setScheme("http")
          .setPort(webserverPort())
          .build();
    } catch (URISyntaxException | UnknownHostException e) {
      // this should be impossible
      throw new RuntimeException(e);
    }
  }

  public URI serviceURI() {
    try {
      return URIBuilder.localhost()
          .appendPath(SERVICE_PATH)
          .setScheme("http")
          .setPort(servicePort())
          .build();
    } catch (URISyntaxException | UnknownHostException e) {
      // this should be impossible
      throw new RuntimeException(e);
    }
  }

  public @NewInstance ServiceTestHarness<T> withLLMPlugin(LLMPlugin<T> plugin) {
    return new ServiceTestHarness<>(chatStore, handler, plugin);
  }

  public @NewInstance ServiceTestHarness<T> withChatStore(ChatStore<T> chatStore) {
    return new ServiceTestHarness<>(chatStore, handler, llmPlugin);
  }

  public @This ServiceTestHarness<T> start() {
    javalin.start(0);
    runner.port(0).start();
    return this;
  }

  public @This ServiceTestHarness<T> stop() {
    runner.close();
    javalin.close();
    return this;
  }

  public String verifyToken() {
    return VERIFY_TOKEN;
  }

  public String accessToken() {
    return ACCESS_TOKEN;
  }

  public String appSecret() {
    return APP_SECRET;
  }

  public ChatStore<T> chatStore() {
    return chatStore;
  }

  public MessageHandler<T> handler() {
    return handler;
  }

  public LLMPlugin<T> llmPlugin() {
    return llmPlugin;
  }

  public String servicePath() {
    return SERVICE_PATH;
  }

  public String dummyPluginResponseText() {
    return ((DummyLLMPlugin<T>) llmPlugin).dummyResponse();
  }

  public int servicePort() {
    return runner.port();
  }

  public int webserverPort() {
    return javalin.port();
  }

  public ServiceTestHarness.@Nullable ReceivedRequest pollWebserver(long milliseconds)
      throws InterruptedException {
    return receivedRequests.poll(milliseconds, TimeUnit.MILLISECONDS);
  }

  public ServiceTestHarness.@Nullable ReceivedRequest pollWebserver() {
    return receivedRequests.poll();
  }

  public record ReceivedRequest(
      String path,
      String body,
      @Nullable String contentType,
      Map<String, String> headerMap,
      Map<String, java.util.List<String>> stringListMap) {}
}
