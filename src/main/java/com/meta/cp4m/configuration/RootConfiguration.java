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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RootConfiguration {
  private final Map<String, PluginConfig> plugins;
  private final Map<String, StoreConfig> stores;
  private final Map<String, HandlerConfig> handlers;
  private final Map<String, PreProcessorConfig> preProcessors;
  private final Collection<ServiceConfiguration> services;

  private final int port;
  private final String heartbeatPath;
  private final Level logLevel;

  @JsonCreator
  RootConfiguration(
      @JsonProperty("plugins") Collection<PluginConfig> plugins,
      @JsonProperty("stores") @Nullable Collection<StoreConfig> stores,
      @JsonProperty("handlers") Collection<HandlerConfig> handlers,
      @JsonProperty("pre_processors") Collection<PreProcessorConfig> preProcessors,
      @JsonProperty("services") Collection<ServiceConfiguration> services,
      @JsonProperty("port") @Nullable Integer port,
      @JsonProperty("heartbeat_path") @Nullable String heartbeatPath,
      @JsonProperty("log_level") @Nullable Level logLevel) {

    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    loggerConfig.setLevel(logLevel);
    ctx.updateLoggers();

    this.port = port == null ? 8080 : port;
    this.heartbeatPath = heartbeatPath == null ? "/heartbeat" : heartbeatPath;
    this.logLevel = Objects.requireNonNullElse(logLevel, Level.INFO);
    stores = stores == null ? Collections.emptyList() : stores;
    preProcessors = preProcessors == null ? Collections.emptyList() : preProcessors;
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

    this.preProcessors =
        preProcessors.stream()
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
      for (PreProcessorConfig preProcessor : preProcessors) {
        Preconditions.checkArgument(
            this.preProcessors.containsKey(preProcessor.name()),
            preProcessor.name() + " must be the name of a pre-processor");
      }
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
      store = new NullStore<>();
    }

    PreProcessor<T> preProcessor;
    List<PreProcessor<T>> preProcessorsList = new ArrayList<>();

    List<String> preProcessorNames = serviceConfig.preProcessors();
    for (String i : preProcessorNames) {
      // guaranteed to return a non-null value due to check in constructor
      preProcessor = preProcessors.get(i).toPreProcessor();
      preProcessorsList.add(preProcessor);
    }

    return new Service<>(store, handler, plugin, preProcessorsList, serviceConfig.webhookPath());
  }

  public ServicesRunner toServicesRunner() {
    ServicesRunner runner = ServicesRunner.newInstance().port(port).heartbeatPath(heartbeatPath);
    for (ServiceConfiguration service : services) {
      MessageHandler<?> handler = handlers.get(service.handler()).toMessageHandler();
      runner.service(createService(handler, service));
    }
    return runner;
  }

  public Level logLevel() {
    return logLevel;
  }
}
