/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.meta.cp4m.DummyWebServer;
import com.meta.cp4m.DummyWebServer.ReceivedRequest;
import com.meta.cp4m.Service;
import com.meta.cp4m.ServicesRunner;
import com.meta.cp4m.llm.DummyLLMPlugin;
import com.meta.cp4m.llm.LLMPlugin;
import com.meta.cp4m.store.ChatStore;
import com.meta.cp4m.store.MemoryStoreConfig;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;

public class ServiceTestHarness<T extends Message> {
  private static final String VERIFY_TOKEN = "test_verify_token";
  private static final String ACCESS_TOKEN = "test_access_token";
  private static final String APP_SECRET = "test_app_secret";
  private static final String SERVICE_PATH = "/testservice";
  private static final String WEBSERVER_PATH = "/testserver";
  private final ChatStore<T> chatStore;
  private final MessageHandler<T> handler;
  private final LLMPlugin<T> llmPlugin;
  private final Service<T> service;
  private final ServicesRunner runner;
  private final DummyWebServer dummyWebServer = DummyWebServer.create();

  private ServiceTestHarness(
      ChatStore<T> chatStore, MessageHandler<T> handler, LLMPlugin<T> llmPlugin) {
    this.chatStore = chatStore;
    this.handler = handler;
    this.llmPlugin = llmPlugin;
    this.service = new Service<>(chatStore, handler, llmPlugin, SERVICE_PATH);
    this.runner = ServicesRunner.newInstance().service(service);
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
      return URIBuilder.loopbackAddress()
          .appendPath(SERVICE_PATH)
          .setScheme("http")
          .setPort(servicePort())
          .build();
    } catch (URISyntaxException e) {
      // this should be impossible
      throw new RuntimeException(e);
    }
  }

  public @This ServiceTestHarness<T> start() {
    runner.port(0).start();
    return this;
  }

  public @This ServiceTestHarness<T> stop() {
    runner.close();
    dummyWebServer.close();
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
    return dummyWebServer.port();
  }

  public @Nullable ReceivedRequest pollWebserver(long milliseconds) throws InterruptedException {
    return dummyWebServer.poll(milliseconds);
  }
}
