/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.knuddels.jtokkit.api.ModelType;

public enum OpenAIModel {
  GPT4 {
    OpenAIModelProperties properties() {
      return new OpenAIModelProperties("gpt-4", 8_192, ModelType.GPT_4);
    }
  },

  GPT432K {
    @Override
    OpenAIModelProperties properties() {
      return new OpenAIModelProperties("gpt-4-32k", 32_768, ModelType.GPT_4_32K);
    }
  },
  GPT35TURBO {
    @Override
    OpenAIModelProperties properties() {
      return new OpenAIModelProperties("gpt-3.5-turbo", 4_096, ModelType.GPT_3_5_TURBO);
    }
  },

  GPT35TURBO16K {
    @Override
    OpenAIModelProperties properties() {
      return new OpenAIModelProperties("gpt-3.5-turbo-16k", 16_384, ModelType.GPT_3_5_TURBO_16K);
    }
  };

  @Override
  public String toString() {
    return this.properties().name();
  }

  abstract OpenAIModelProperties properties();

  public record OpenAIModelProperties(String name, long tokenLimit, ModelType jtokkinModel) {}
}
