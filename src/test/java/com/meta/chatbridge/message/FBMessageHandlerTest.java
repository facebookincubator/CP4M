/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.meta.chatbridge.FBID;
import com.meta.chatbridge.Pipeline;
import com.meta.chatbridge.PipelinesRunner;
import com.meta.chatbridge.llm.DummyLLMHandler;
import com.meta.chatbridge.store.MemoryStore;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FBMessageHandlerTest {

  private static final FBMessage DUMMY_ASSISTANT_MESSAGE =
      new FBMessage(
          Instant.now(),
          FBID.from(1),
          FBID.from(1),
          FBID.from(1),
          "this is a dummy message",
          Message.Role.ASSISTANT);

  @Test
  void inbound() {
    Pipeline<FBMessage> pipeline =
        new Pipeline<>(
            new MemoryStore<>(),
            new FBMessageHandler(),
            new DummyLLMHandler<>(DUMMY_ASSISTANT_MESSAGE),
            "/testfbmessage");
    PipelinesRunner runner = PipelinesRunner.newInstance().pipeline(pipeline).port(0);
    runner.start();
  }
}
