/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.message;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.checkerframework.common.reflection.qual.NewInstance;

public class TextChunker {

  private final long maxCharsPerChunk;
  private final List<Pattern> regex;

  private TextChunker(long maxCharsPerChunk, List<Pattern> regex) {
    this.maxCharsPerChunk = maxCharsPerChunk;
    this.regex = regex;
  }

  public static TextChunker from(long maxCharsPerChunk) {
    return new TextChunker(maxCharsPerChunk, Collections.emptyList());
  }

  public @NewInstance TextChunker withSeparator(String regex) {
    return withSeparator(Pattern.compile(regex));
  }

  public @NewInstance TextChunker withSeparator(Pattern regex) {
    ImmutableList<Pattern> newRegex =
        ImmutableList.<Pattern>builder().addAll(this.regex).add(regex).build();

    return new TextChunker(maxCharsPerChunk, newRegex);
  }

  private Stream<String> chunker(String text, Pattern regex) {
    if (text.length() > maxCharsPerChunk) {
      return Arrays.stream(regex.split(text, 0));
    }
    return Stream.of(text);
  }

  public Stream<String> chunks(String text) {
    Stream<String> stream = Stream.of(text.strip());
    for (Pattern r : regex) {
      stream = stream.flatMap(t -> chunker(t, r));
    }

    return stream;
  }
}
