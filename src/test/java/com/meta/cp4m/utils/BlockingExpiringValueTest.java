/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BlockingExpiringValueTest {

  @Test
  void test() throws InterruptedException {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    BlockingExpiringValue<String> value =
        new BlockingExpiringValue<>(Instant.MAX, "valid").clock(clock);
    assertThat(value.get()).isEqualTo("valid");
    assertThat(value.get()).isEqualTo("valid"); // can be done multiple times

    value.update(Instant.MIN, "invalid");
    Future<String> v = executor.submit(value::get);
    assertThat(v.isDone()).isFalse();
    value.update(clock.instant().plusSeconds(1), "valid");
    assertThat(v)
        .satisfies(f -> assertThat(f).succeedsWithin(300, TimeUnit.MILLISECONDS))
        .satisfies(f -> assertThat(f.get()).isEqualTo("valid"));
    value.clock(Clock.offset(clock, Duration.ofSeconds(2)));
    v = executor.submit(value::get);
    assertThat(v)
        .satisfies(f -> assertThat(f.isCancelled()).isFalse())
        .satisfies(f -> assertThat(f.isDone()).isFalse())
        .failsWithin(1, TimeUnit.MILLISECONDS);
  }
}
