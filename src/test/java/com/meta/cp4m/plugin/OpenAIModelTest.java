/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OpenAIModelTest {

  /** This is super important because the JsonDeserializer relies on it */
  @Test
  void namesAreUnique() {
    Set<String> uniqueElements =
        Arrays.stream(OpenAIModel.values()).map(OpenAIModel::toString).collect(Collectors.toSet());
    assertThat(OpenAIModel.values()).extracting(Enum::toString).hasSize(uniqueElements.size());
  }

  /** This is super important because the JsonDeserializer relies on it */
  @Test
  void toStringMatchesName() {
    assertThat(OpenAIModel.values())
        .allSatisfy(v -> assertThat(v.properties().name()).isEqualTo(v.toString()));
  }
}
