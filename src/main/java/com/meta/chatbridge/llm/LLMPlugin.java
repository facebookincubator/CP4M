/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.message.MessageStack;
import java.io.IOException;

public interface LLMPlugin<T extends Message> {

  T handle(MessageStack<T> messageStack) throws IOException;
}
