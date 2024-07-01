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
import com.meta.cp4m.*;
import com.meta.cp4m.message.HandlerConfig;
import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.MessageHandler;
import com.meta.cp4m.plugin.Plugin;
import com.meta.cp4m.plugin.PluginConfig;
import com.meta.cp4m.store.ChatStore;
import com.meta.cp4m.store.NullStore;
import com.meta.cp4m.store.StoreConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RootConfiguration {
  private final Map<String, PluginConfig> plugins;
  private final Map<String, StoreConfig> stores;
  private final Map<String, HandlerConfig> handlers;
  private final Map<String, PreProcessorConfig> preProcessors;
  private final Collection<ServiceConfiguration> services;

  private final int port;
  private final String heartbeatPath;

  @JsonCreator
  RootConfiguration(
      @JsonProperty("plugins") Collection<PluginConfig> plugins,
      @JsonProperty("stores") @Nullable Collection<StoreConfig> stores,
      @JsonProperty("handlers") Collection<HandlerConfig> handlers,
      @JsonProperty("preProcessors") @Nullable Collection<PreProcessorConfig> preProcessors,
      @JsonProperty("services") Collection<ServiceConfiguration> services,
      @JsonProperty("port") @Nullable Integer port,
      @JsonProperty("heartbeat_path") @Nullable String heartbeatPath) {
    this.port = port == null ? 8080 : port;
    this.heartbeatPath = heartbeatPath == null ? "/heartbeat" : heartbeatPath;
    stores = stores == null ? Collections.emptyList() : stores;
    preProcessors = preProcessors == null ? Collections.emptyList(): preProcessors;
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
            == plugins.stream()
                .map(PluginConfig::name)
                .collect(Collectors.toUnmodifiableSet())
                .size(),
        "all plugin names must be unique");
    this.plugins =
        plugins.stream()
            .collect(Collectors.toUnmodifiableMap(PluginConfig::name, Function.identity()));

    this.preProcessors = preProcessors.stream()
            .collect(Collectors.toUnmodifiableMap(PreProcessorConfig::name, Function.identity()));

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

  Collection<PluginConfig> plugins() {
    return Collections.unmodifiableCollection(plugins.values());
  }

  Collection<PreProcessorConfig> preProcessors() {
    return Collections.unmodifiableCollection(preProcessors.values());
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
    Plugin<T> plugin = plugins.get(serviceConfig.plugin()).toPlugin();
    ChatStore<T> store;
    if (serviceConfig.store() != null) {
      store = stores.get(serviceConfig.store()).toStore();
    } else {
      store = new NullStore<T>();
    }

    PreProcessor<T> preProcessor;
    if(serviceConfig.preProcessor() != null){
      preProcessor = preProcessors.get(serviceConfig.preProcessor()).toPreProcessor();
      return new Service<>(store, handler, plugin, List.of(preProcessor), serviceConfig.webhookPath());
    } else {
      // TODO: flow never goes into else. Need to debug why method inherits container annotation
      return new Service<>(store, handler, plugin, serviceConfig.webhookPath());
    }
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
