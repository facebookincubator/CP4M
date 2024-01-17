/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.meta.cp4m.Identifier;
import com.meta.cp4m.message.Message.Role;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FunctionalInterface
public interface MessageFactory<T extends Message> {
  Map<Class<? extends Message>, MessageFactory<? extends Message>> FACTORY_MAP =
      Stream.<FactoryContainer<?>>of(
              new FactoryContainer<>(
                  FBMessage.class, (t, m, si, ri, ii, r, pm) -> new FBMessage(t, ii, si, ri, m, r, pm)),
              new FactoryContainer<>(
                  WAMessage.class, (t, m, si, ri, ii, r, pm) -> new WAMessage(t, ii, si, ri, m, r, pm)))
          .collect(
              Collectors.toUnmodifiableMap(FactoryContainer::clazz, FactoryContainer::factory));

  static <T extends Message> MessageFactory<T> instance(Class<T> clazz) {
    @SuppressWarnings("unchecked") // static map guarantees this to be true
    MessageFactory<T> factory = (MessageFactory<T>) FACTORY_MAP.get(clazz);
    Objects.requireNonNull(factory, clazz + " does not have a registered factory");
    return factory;
  }

  static <T extends Message> MessageFactory<T> instance(T message) {
    @SuppressWarnings("unchecked") // class of an object is its class :)
    Class<T> clazz = (Class<T>) message.getClass();
    return instance(clazz);
  }

  T newMessage(
      Instant timestamp,
      String message,
      Identifier senderId,
      Identifier recipientId,
      Identifier instanceId,
      Role role,
      Message parentMessage);

  /** this exists to provide compiler guarantees for type matching in the FACTORY_MAP */
  record FactoryContainer<T extends Message>(Class<T> clazz, MessageFactory<T> factory) {}
}
