/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class Identifier implements Comparable<Identifier> {

  private final byte[] id;

  private Identifier(byte[] id) {
    this.id = id;
  }

  public static Identifier from(String id) {
    return new Identifier(id.getBytes(StandardCharsets.UTF_8));
  }

  public static Identifier from(long id) {
    return new Identifier(Long.toString(id).getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String toString() {
    return new String(id, StandardCharsets.UTF_8);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Identifier that = (Identifier) o;
    return Arrays.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(id);
  }

  @Override
  public int compareTo(@NotNull Identifier o) {
    Objects.requireNonNull(o);
    return toString().compareTo(o.toString());
  }
}
