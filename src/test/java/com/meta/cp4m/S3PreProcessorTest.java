/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import com.meta.cp4m.plugin.DummyPlugin;
import com.meta.cp4m.message.WAMessage;
import com.meta.cp4m.message.WAMessageHandler;
import com.meta.cp4m.message.WAMessengerConfig;
import com.meta.cp4m.store.NullStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class S3PreProcessorTest {

    //    @Test
    void run() {
        WAMessageHandler waMessageHandler = WAMessengerConfig.of("verify","", "") //TODO: use credentials from config
                .toMessageHandler();
        Service<WAMessage> service = new Service<>(new NullStore<>(), waMessageHandler, new DummyPlugin<>("dummy"), List.of(new S3PreProcessor<>()), "/whatsapp");
        ServicesRunner.newInstance().service(service).port(8080).start();
    }
}