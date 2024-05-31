/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.metrics;

import com.meta.cp4m.llm.OpenAIPlugin;
import com.meta.cp4m.message.MessageHandler;

import java.util.UUID;

public interface MetricsCollector {

    <T extends MessageHandler<?>, E extends OpenAIPlugin<?>> void logMessageReceived(String name, UUID instanceId, Class<T> messengerType, Class<E> pluginType, String appId, String businessContactIdentifier);

    <T extends MessageHandler<?>, E extends OpenAIPlugin<?>> void logMessageSent(String name, UUID instanceId, Class<T> messengerType, Class<E> pluginType, String appId, String businessContactIdentifier);

}
