/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.meta.cp4m.Service;
import com.meta.cp4m.ServiceConfiguration;
import com.meta.cp4m.ServicesRunner;
import com.meta.cp4m.llm.LLMConfig;
import com.meta.cp4m.llm.LLMPlugin;
import com.meta.cp4m.message.HandlerConfig;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.MessageHandler;
import com.meta.cp4m.store.ChatStore;
import com.meta.cp4m.store.NullStore;
import com.meta.cp4m.store.StoreConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RootConfiguration {
  private final Map<String, LLMConfig> plugins;
  private final Map<String, StoreConfig> stores;
  private final Map<String, HandlerConfig> handlers;
  private final Collection<ServiceConfiguration> services;

  private final int port;
  private final String heartbeatPath;

  @JsonCreator
  RootConfiguration(
      @JsonProperty("plugins") Collection<LLMConfig> plugins,
      @JsonProperty("stores") @Nullable Collection<StoreConfig> stores,
      @JsonProperty("handlers") Collection<HandlerConfig> handlers,
      @JsonProperty("services") Collection<ServiceConfiguration> services,
      @JsonProperty("port") @Nullable Integer port,
      @JsonProperty("heartbeat_path") @Nullable String heartbeatPath) {
    this.port = port == null ? 8080 : port;
    this.heartbeatPath = heartbeatPath == null ? "/heartbeat" : heartbeatPath;
    stores = stores == null ? Collections.emptyList() : stores;
    Preconditions.checkArgument(
        this.port >= 0 && this.port <= 65535, "port must be between 0 and 65535");

    Preconditions.checkArgument(
        plugins != null && !plugins.isEmpty(), "At least one plugin must defined");
    Preconditions.checkArgument(
        handlers != null && !handlers.isEmpty(), "at least one handler must be defined");
    Preconditions.checkArgument(
        services != null && !services.isEmpty(), "at least one service must be defined");

    Preconditions.checkArgument(
        plugins.size()
            == plugins.stream().map(LLMConfig::name).collect(Collectors.toUnmodifiableSet()).size(),
        "all plugin names must be unique");
    this.plugins =
        plugins.stream()
            .collect(Collectors.toUnmodifiableMap(LLMConfig::name, Function.identity()));

    Preconditions.checkArgument(
        stores.size()
            == stores.stream()
                .map(StoreConfig::name)
                .collect(Collectors.toUnmodifiableSet())
                .size(),
        "all store names must be unique");
    this.stores =
        stores.stream()
            .collect(Collectors.toUnmodifiableMap(StoreConfig::name, Function.identity()));

    Preconditions.checkArgument(
        handlers.size()
            == handlers.stream()
                .map(HandlerConfig::name)
                .collect(Collectors.toUnmodifiableSet())
                .size(),
        "all handler names must be unique");
    this.handlers =
        handlers.stream()
            .collect(Collectors.toUnmodifiableMap(HandlerConfig::name, Function.identity()));

    for (ServiceConfiguration s : services) {
      Preconditions.checkArgument(
          this.plugins.containsKey(s.plugin()), s.plugin() + " must be the name of a plugin");
      Preconditions.checkArgument(
          s.store() == null || this.stores.containsKey(s.store()),
          s.store() + " must be the name of a store");
      Preconditions.checkArgument(
          this.handlers.containsKey(s.handler()), s.handler() + " must be the name of a handler");
    }
    this.services = services;
  }

  Collection<LLMConfig> plugins() {
    return Collections.unmodifiableCollection(plugins.values());
  }

  Collection<StoreConfig> stores() {
    return Collections.unmodifiableCollection(stores.values());
  }

  Collection<HandlerConfig> handlers() {
    return Collections.unmodifiableCollection(handlers.values());
  }

  Collection<ServiceConfiguration> services() {
    return Collections.unmodifiableCollection(services);
  }

  public int port() {
    return port;
  }

  public String heartbeatPath() {
    return heartbeatPath;
  }

  private <T extends Message> Service<T> createService(
      MessageHandler<T> handler, ServiceConfiguration serviceConfig) {
    LLMPlugin<T> plugin = plugins.get(serviceConfig.plugin()).toPlugin();
    ChatStore<T> store;
    if (serviceConfig.store() != null) {
      store = stores.get(serviceConfig.store()).toStore();
    } else {
      store = new NullStore<T>();
    }
    return new Service<>(store, handler, plugin, serviceConfig.webhookPath());
  }

  public ServicesRunner toServicesRunner() {
    ServicesRunner runner = ServicesRunner.newInstance().port(port).heartbeatPath(heartbeatPath);
    for (ServiceConfiguration service : services) {
      MessageHandler<?> handler = handlers.get(service.handler()).toMessageHandler();
      runner.service(createService(handler, service));
    }
    return runner;
  }
}
