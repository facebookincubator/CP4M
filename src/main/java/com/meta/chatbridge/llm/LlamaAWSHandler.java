/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.meta.chatbridge.message.Message;
import com.meta.chatbridge.store.LLMContextManager;
import com.meta.chatbridge.store.MessageStack;

public class LlamaAWSHandler implements LLMHandler {

    private final LLMContextManager context;

    public LlamaAWSHandler(LLMContextManager context) {
        this.context = context;
    }

    @Override
    public Message handle(MessageStack messageStack) {
//        Take history
//        get number of tokens and truncate as needed
//        Pass to LLM
//        Return response
        Message message = (Message) messageStack.messages().get(0);

        return null;

    }
}
