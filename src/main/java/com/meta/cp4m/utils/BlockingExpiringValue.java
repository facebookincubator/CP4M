/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.utils;

import java.time.Clock;
import java.time.Instant;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.jetbrains.annotations.TestOnly;

public class BlockingExpiringValue<T> {

  private final Object lock = new Object();
  private Clock clock = Clock.systemUTC();
  private volatile Instant expiry;
  private volatile T value;

  public BlockingExpiringValue(Instant expiry, T value) {
    this.expiry = expiry;
    this.value = value;
  }

  @TestOnly
  @This
  BlockingExpiringValue<T> clock(Clock clock) {
    this.clock = clock;
    return this;
  }

  public void update(Instant expiry, T value) {
    synchronized (lock) {
      this.expiry = expiry;
      this.value = value;
      lock.notifyAll();
    }
  }

  public T get() throws InterruptedException {
    synchronized (lock) {
      while (clock.instant().isAfter(expiry)) {
        lock.wait();
      }
    }
    return value;
  }
}
