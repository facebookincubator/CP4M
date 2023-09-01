/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

public enum OpenAIModel {
  GPT4 {
    OpenAIModelProperties properties() {
      return new OpenAIModelProperties("gpt-4", 8_192);
    }
  },

  GPT432K {
    @Override
    OpenAIModelProperties properties() {
      return new OpenAIModelProperties("gpt-4-32k", 32_768);
    }
  },
  GPT35TURBO {
    @Override
    OpenAIModelProperties properties() {
      return new OpenAIModelProperties("gpt-3.5-turbo", 4_096);
    }
  },

  GPT35TURBO16K {
    @Override
    OpenAIModelProperties properties() {
      return new OpenAIModelProperties("gpt-3.5-turbo-16k", 16_384);
    }
  };

  @Override
  public String toString() {
    return this.properties().name();
  }

  abstract OpenAIModelProperties properties();

  public record OpenAIModelProperties(String name, long tokenLimit) {}
}
