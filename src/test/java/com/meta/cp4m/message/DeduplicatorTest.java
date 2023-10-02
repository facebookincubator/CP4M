/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DeduplicatorTest {

  @Test
  void exercise() {
    Deduplicator<String> deduplicator = new Deduplicator<>(5);
    Stream.of("1", "2", "3", "4", "5")
        .forEachOrdered(
            v -> {
              assertThat(deduplicator.addAndGetIsDuplicate(v)).isFalse();
              assertThat(deduplicator.addAndGetIsDuplicate(v)).isTrue();
              assertThat(deduplicator.size()).isEqualTo(Integer.valueOf(v));
            });

    Stream.of("1", "2", "3", "4", "5")
        .forEachOrdered(
            v -> {
              assertThat(deduplicator.addAndGetIsDuplicate(v))
                  .overridingErrorMessage(v + " was found to be missing when it should be present")
                  .isTrue();
              assertThat(deduplicator.size()).isEqualTo(5);
            });

    assertThat(deduplicator.addAndGetIsDuplicate("6")).isFalse();
    assertThat(deduplicator.addAndGetIsDuplicate("6")).isTrue();
    assertThat(deduplicator.size()).isEqualTo(5);
    assertThat(deduplicator.addAndGetIsDuplicate("1")).isFalse();
  }
}
