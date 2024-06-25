/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.junit.jupiter.api.Test;

class ServicesRunnerTest {
  @Test
  void heartbeat() throws URISyntaxException, IOException {
    ServicesRunner runner = ServicesRunner.newInstance().port(0).start();
    URI url =
        URIBuilder.loopbackAddress()
            .setPort(runner.port())
            .setScheme("http")
            .appendPath("heartbeat")
            .build();
    HttpResponse res = Request.get(url).execute().returnResponse();
    assertThat(res.getCode()).isEqualTo(200);
  }

  @Test
  void heartbeatCustomPath() throws URISyntaxException, IOException {
    ServicesRunner runner = ServicesRunner.newInstance().port(0).heartbeatPath("custom").start();
    URI url =
        URIBuilder.loopbackAddress()
            .setPort(runner.port())
            .setScheme("http")
            .appendPath("custom")
            .build();
    HttpResponse res = Request.get(url).execute().returnResponse();
    assertThat(res.getCode()).isEqualTo(200);
    res = Request.get(URIBuilder.loopbackAddress()
        .setPort(runner.port())
        .setScheme("http")
        .appendPath("heartbeat")
        .build()).execute().returnResponse();
    assertThat(res.getCode()).isEqualTo(404);
  }
}
