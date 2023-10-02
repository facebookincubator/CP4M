/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.checkerframework.common.reflection.qual.NewInstance;

/**
 * Splits texts into 'chunks' of test with less than or equal to the requested number of characters.
 * The text block is split first on the regex separators, in the defined order, each separator is
 * only used to split the chunk if the chunk exceeds the defined maximum numbers of characters per
 * chunk. If all separators are exhausted and a chunk still exceeds the maximum number of characters
 * allowed it is split without regard for any separator into chunks less than or equal to the
 * maximum character size.
 */
public class TextChunker {

  private final int maxCharsPerChunk;
  private final List<Pattern> regex;

  private TextChunker(int maxCharsPerChunk, List<Pattern> regex) {
    Preconditions.checkArgument(maxCharsPerChunk > 0);
    this.maxCharsPerChunk = maxCharsPerChunk;
    this.regex = regex;
  }

  public static TextChunker from(int maxCharsPerChunk) {
    return new TextChunker(maxCharsPerChunk, Collections.emptyList());
  }

  /**
   * A separator with sensible separators, the separators are applied in this order.
   *
   * <pre>{@code
   * from(maxCharsPerChunk)
   *       .withSeparator("\n\n\n+")
   *       .withSeparator("\n\n")
   *       .withSeparator("\n")
   *       .withSeparator("\\. +") // any period, including the following whitespaces
   *       .withSeparator("\s\s+") // any set of two or more whitespace characters
   *       .withSeparator(" +"); // any set of one or more whitespace spaces
   * }</pre>
   *
   * @return stream of text chunks less than or equal to the maxCharsPerChunk
   */
  public static TextChunker standard(int maxCharsPerChunk) {
    return from(maxCharsPerChunk)
        .withSeparator("\n\n\n+")
        .withSeparator("\n\n")
        .withSeparator("\n")
        .withSeparator("\\. +") // any period, including the following whitespaces
        .withSeparator("\s\s+") // any set of two or more whitespace characters
        .withSeparator(" +"); // any set of one or more whitespace spaces
  }

  public @NewInstance TextChunker withSeparator(String regex) {
    return withSeparator(Pattern.compile(regex));
  }

  public @NewInstance TextChunker withSeparator(Pattern regex) {
    ImmutableList<Pattern> newRegex =
        ImmutableList.<Pattern>builder().addAll(this.regex).add(regex).build();

    return new TextChunker(maxCharsPerChunk, newRegex);
  }

  private Stream<String> breaker(String text) {
    ArrayList<String> out = new ArrayList<>((text.length() / maxCharsPerChunk) + 1);
    while (text.length() > maxCharsPerChunk) {
      out.add(text.substring(0, maxCharsPerChunk));
      text = text.substring(maxCharsPerChunk);
    }
    out.add(text);
    return out.stream();
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
    stream = stream.flatMap(this::breaker);

    return stream;
  }
}
