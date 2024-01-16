/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.store;

import com.meta.cp4m.message.Message;
import com.meta.cp4m.message.ThreadState;
import java.util.List;

/**
 * This class is in charge of both maintaining a chat history and managing a queue of conversations
 * that require a response.
 *
 * <p>Adding a message from {@link com.meta.cp4m.message.Message.Role#USER} will indicate that the
 * conversation needs a response.
 *
 * @param <T> the type of message being passed
 */
public interface ChatStore<T extends Message> {

  ThreadState<T> add(T message);

  long size();

  List<ThreadState<T>> list();
}
