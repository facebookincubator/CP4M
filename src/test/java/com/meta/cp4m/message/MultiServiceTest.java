/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meta.cp4m.DummyWebServer;
import com.meta.cp4m.DummyWebServer.ReceivedRequest;
import com.meta.cp4m.Identifier;
import com.meta.cp4m.Service;
import com.meta.cp4m.ServicesRunner;
import com.meta.cp4m.llm.DummyLLMPlugin;
import com.meta.cp4m.store.MemoryStore;
import com.meta.cp4m.store.MemoryStoreConfig;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.Method;
import org.junit.jupiter.api.Test;

public class MultiServiceTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String META_PATH = "/meta";
  private final DummyWebServer metaWebServer = DummyWebServer.create();

  private final Function<Identifier, URI> baseURLFactory =
      HandlerTestUtils.baseURLFactory(META_PATH, metaWebServer.port());

  @Test
  void waAndFBTest() throws Exception {
    final String path = "/path";
    final String fb1VerifyToken = "fb1VerifyToken";
    final String fb1AppSecret = "fb1AppSecret";
    final String fb1PageAccessToken = "fb1PageAccessToken";

    MemoryStore<FBMessage> fb1Store = MemoryStoreConfig.of(1, 1).toStore();
    FBMessageHandler fb1Handler =
        FBMessengerConfig.of(fb1VerifyToken, fb1AppSecret, fb1PageAccessToken)
            .toMessageHandler()
            .baseURLFactory(baseURLFactory);
    DummyLLMPlugin<FBMessage> fb1Plugin = new DummyLLMPlugin<>("i'm a fb1 dummy");
    Service<FBMessage> fb1Service = new Service<>(fb1Store, fb1Handler, fb1Plugin, path);

    final String fb2VerifyToken = "fb2VerifyToken";
    final String fb2AppSecret = "fb2AppSecret";
    final String fb2PageAccessToken = "fb2PageAccessToken";

    MemoryStore<FBMessage> fb2Store = MemoryStoreConfig.of(1, 1).toStore();
    FBMessageHandler fb2Handler =
        FBMessengerConfig.of(fb2VerifyToken, fb2AppSecret, fb2PageAccessToken)
            .toMessageHandler()
            .baseURLFactory(baseURLFactory);
    DummyLLMPlugin<FBMessage> fb2Plugin = new DummyLLMPlugin<>("i'm a fb2 dummy");
    Service<FBMessage> fb2Service = new Service<>(fb2Store, fb2Handler, fb2Plugin, path);

    final String wa1VerifyToken = "wa1VerifyToken";
    final String wa1AppSecret = "wa1AppSecret";
    final String wa1PageAccessToken = "wa1PageAccessToken";
    MemoryStore<WAMessage> wa1Store = MemoryStoreConfig.of(1, 1).toStore();
    WAMessageHandler wa1Handler =
        WAMessengerConfig.of(wa1VerifyToken, wa1AppSecret, wa1PageAccessToken)
            .toMessageHandler()
            .baseUrlFactory(baseURLFactory);
    DummyLLMPlugin<WAMessage> wa1Plugin = new DummyLLMPlugin<>("i'm a wa1 dummy");
    Service<WAMessage> wa1Service = new Service<>(wa1Store, wa1Handler, wa1Plugin, path);

    final String wa2VerifyToken = "wa2VerifyToken";
    final String wa2AppSecret = "wa2AppSecret";
    final String wa2PageAccessToken = "wa2PageAccessToken";
    MemoryStore<WAMessage> wa2Store = MemoryStoreConfig.of(1, 1).toStore();
    WAMessageHandler wa2Handler =
        WAMessengerConfig.of(wa2VerifyToken, wa2AppSecret, wa2PageAccessToken)
            .toMessageHandler()
            .baseUrlFactory(baseURLFactory);
    DummyLLMPlugin<WAMessage> wa2Plugin = new DummyLLMPlugin<>("i'm a wa2 dummy");
    Service<WAMessage> wa2Service = new Service<>(wa2Store, wa2Handler, wa2Plugin, path);

    ServicesRunner runner =
        ServicesRunner.newInstance()
            .service(fb1Service)
            .service(fb2Service)
            .service(wa1Service)
            .service(wa2Service)
            .port(0)
            .start();

    // FB1 test
    Function<JsonNode, Request> fb1RequestFactory =
        HandlerTestUtils.MessageRequestFactory(Method.POST, path, fb1AppSecret, runner.port());
    fb1RequestFactory.apply(FBMessageHandlerTest.PARSED_SAMPLE_MESSAGE).execute();
    fb1Plugin.take(500);
    ReceivedRequest receivedRequest = metaWebServer.take(500);
    assertThat(receivedRequest.path()).isEqualTo(META_PATH);
    assertThat(receivedRequest.body()).contains("i'm a fb1 dummy");

    // FB2 test
    Function<JsonNode, Request> fb2RequestFactory =
        HandlerTestUtils.MessageRequestFactory(Method.POST, path, fb2AppSecret, runner.port());
    fb2RequestFactory.apply(FBMessageHandlerTest.PARSED_SAMPLE_MESSAGE).execute();
    fb2Plugin.take(500);
    receivedRequest = metaWebServer.take(500);
    assertThat(receivedRequest.path()).isEqualTo(META_PATH);
    assertThat(receivedRequest.body()).contains("i'm a fb2 dummy");

    // WA1 test
    Function<JsonNode, Request> wa1RequestFactory =
        HandlerTestUtils.MessageRequestFactory(Method.POST, path, wa1AppSecret, runner.port());
    wa1RequestFactory.apply(MAPPER.readTree(WAMessageHandlerTest.VALID)).execute();
    wa1Plugin.take(500);
    receivedRequest = metaWebServer.take(500);
    ReceivedRequest receivedRequest2 = metaWebServer.take(500);
    assertThat(receivedRequest.path()).isEqualTo(META_PATH);
    assertThat(receivedRequest2.path()).isEqualTo(META_PATH);
    assertThat(List.of(receivedRequest, receivedRequest2))
        .satisfiesOnlyOnce(r -> assertThat(r.body()).contains("i'm a wa1 dummy"));

    // WA2 test
    Function<JsonNode, Request> wa2RequestFactory =
        HandlerTestUtils.MessageRequestFactory(Method.POST, path, wa2AppSecret, runner.port());
    wa2RequestFactory.apply(MAPPER.readTree(WAMessageHandlerTest.VALID)).execute();
    wa2Plugin.take(500);
    receivedRequest = metaWebServer.take(500);
    receivedRequest2 = metaWebServer.take(500);
    assertThat(receivedRequest.path()).isEqualTo(META_PATH);
    assertThat(receivedRequest2.path()).isEqualTo(META_PATH);
    assertThat(List.of(receivedRequest, receivedRequest2))
        .satisfiesOnlyOnce(r -> assertThat(r.body()).contains("i'm a wa2 dummy"));
  }
}
