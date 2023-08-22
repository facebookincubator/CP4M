/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.meta.chatbridge.FBID;
import com.meta.chatbridge.Pipeline;
import com.meta.chatbridge.PipelinesRunner;
import com.meta.chatbridge.llm.DummyLLMHandler;
import com.meta.chatbridge.store.MemoryStore;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.junit.jupiter.api.Test;

class FBMessageHandlerTest {

  private static final FBMessage DUMMY_ASSISTANT_MESSAGE =
      new FBMessage(
          Instant.now(),
          "1",
          FBID.from(1),
          FBID.from(1),
          "this is a dummy message",
          Message.Role.ASSISTANT);

  private HttpResponse getRequest(String path, int port, Map<String, String> params)
      throws IOException, URISyntaxException {
    URIBuilder uriBuilder = URIBuilder.loopbackAddress().setScheme("http").setPort(port).appendPath(path);
    params.forEach(uriBuilder::addParameter);
    return Request.get(uriBuilder.build()).execute().returnResponse();
  }

  @Test
  void validation() throws IOException, URISyntaxException {
    String token = "243af3c6-9994-4869-ae13-ad61a38323f5";
    int challenge = 1158201444;
    Pipeline<FBMessage> pipeline =
        new Pipeline<>(
            new MemoryStore<>(),
            new FBMessageHandler(token, null),
            new DummyLLMHandler<>(DUMMY_ASSISTANT_MESSAGE),
            "/testfbmessage");
    final PipelinesRunner runner = PipelinesRunner.newInstance().pipeline(pipeline).port(0);
    HttpResponse response;
    try (PipelinesRunner ignored = runner.start()) {
      ImmutableMap<String, String> params =
          ImmutableMap.<String, String>builder()
              .put("hub.mode", "subscribe")
              .put("hub.challenge", Integer.toString(challenge))
              .put("hub.verify_token", token)
              .build();
      response = getRequest("testfbmessage", runner.port(), params);
    }
    assertThat(response.getCode()).isEqualTo(200);
    InputStream inputStream = ((BasicClassicHttpResponse) response).getEntity().getContent();
    byte[] input = new byte[inputStream.available()];
    inputStream.read(input);
    String text = new String(input, StandardCharsets.UTF_8);
    assertThat(text).isEqualTo(Integer.toString(challenge));
  }
}
