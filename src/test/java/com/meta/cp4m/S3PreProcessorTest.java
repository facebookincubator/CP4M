/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import static org.junit.jupiter.api.Assertions.*;

import com.meta.cp4m.message.WAMessage;
import com.meta.cp4m.message.WAMessageHandler;
import com.meta.cp4m.message.WAMessengerConfig;
import com.meta.cp4m.plugin.DummyPlugin;
import com.meta.cp4m.store.NullStore;
import java.util.List;
import org.junit.jupiter.api.Test;

class S3PreProcessorTest {

    @Test
    void run() {
        WAMessageHandler waMessageHandler = WAMessengerConfig.of("verify", "SomeSecret", "someToken")
                .toMessageHandler();
    Service<WAMessage> service =
        new Service<>(
            new NullStore<>(),
            waMessageHandler,
            new DummyPlugin<>("dummy"),
            List.of(
                new S3PreProcessor<>(
                    "someAccessKey", "someSecretKey", "someRegion", "someRegion", null)),
            "/whatsapp");
        ServicesRunner.newInstance().service(service).port(8080).start();
    }
}
