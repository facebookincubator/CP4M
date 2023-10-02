/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentifierTest {

  @Test
  void simple() {
    assertThat(Identifier.from(1L)).isNotEqualTo(Identifier.from(2L));
    assertThat(Identifier.from(1L).hashCode()).isNotEqualTo(Identifier.from(2L).hashCode());
    assertThat(Identifier.from(1L)).isEqualTo(Identifier.from(1L));
    assertThat(Identifier.from(1L).hashCode()).isEqualTo(Identifier.from(1L).hashCode());
  }

  @Test
  void stringAndLongTheSame() {
    assertThat(Identifier.from(1L)).isEqualTo(Identifier.from("1"));
    assertThat(Identifier.from(1L).hashCode()).isEqualTo(Identifier.from("1").hashCode());
  }

  @Test
  void comparison() {
    assertThat(Identifier.from("1")).isGreaterThan(Identifier.from("0"));
    assertThat(Identifier.from("1")).isLessThan(Identifier.from("2"));
  }
}
