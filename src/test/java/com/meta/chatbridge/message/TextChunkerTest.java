/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

  @Test
  void noSeparators() {
    TextChunker chunker = TextChunker.from(10);
    assertThat(chunker.chunks("this is definitely more than ten characters"))
        .hasSize(5)
        .containsSequence("this is de", "finitely m", "ore than t", "en charact", "ers");
  }

  @Test
  void singleSeparator() {
    TextChunker chunker = TextChunker.from(3).withSeparator(" ");
    assertThat(chunker.chunks("123")).hasSize(1).contains("123");
    assertThat(chunker.chunks("123 12 1"))
        .hasSize(3)
        .contains("123", Index.atIndex(0))
        .contains("12", Index.atIndex(1))
        .contains("1", Index.atIndex(2));
  }

  @Test
  void chainedSeparators() {
    TextChunker chunker = TextChunker.from(10).withSeparator("\\. ").withSeparator(" ");
    assertThat(chunker.chunks("i am short. I am longer. longerthantencharacters"))
        .hasSize(7)
        .containsSequence("i am short", "I", "am", "longer", "longerthan", "tencharact", "ers");
  }
}
