/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge;

import java.util.Objects;

public class FBID {

  private final long value;

  private FBID(long value) {
    this.value = value;
  }

  public static FBID from(long value) {
    return new FBID(value);
  }

  public static FBID from(String value) {
    Objects.requireNonNull(value);
    long longValue = Long.valueOf(value);
    return new FBID(longValue);
  }

  public long longValue() {
    return value;
  }

  @Override
  public String toString() {
    return Long.toString(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FBID fbid = (FBID) o;
    return value == fbid.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
