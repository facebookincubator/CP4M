/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.google.common.base.Preconditions;
import java.util.LinkedHashSet;

// TODO: make this not synchronized
public class Deduplicator<T> {

  private final int capacity;
  private final LinkedHashSet<T> set;

  public Deduplicator(int capacity) {
    Preconditions.checkArgument(capacity > 0);
    this.capacity = capacity;
    set = new LinkedHashSet<>(capacity);
  }

  public synchronized boolean addAndGetIsDuplicate(T value) {
    boolean added = set.add(value);
    if (added && set.size() > capacity) {
      set.remove(set.iterator().next());
    }
    return !added;
  }

  public synchronized int size() {
    return set.size();
  }
}
