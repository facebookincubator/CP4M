/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.llm;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import java.util.*;

public class LlamaTokenizer {

    private final int MAX_TOKENS = 4096;
    private final int MAX_RESPONSE_TOKENS = 1024;
    private final int MAX_CONTEXT_TOKENS = 1536;

    private final Encoding tokenizer;

    public LlamaTokenizer() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        tokenizer = registry.getEncoding(EncodingType.CL100K_BASE);
    }


    /**
     * Returns a count of the tokens from the context string
     *
     * @param contextString The context documents
     */
    private int getContextString(String contextString) {
        var tokenCount = 0;

        tokenCount += tokenizer.encode(contextString + "\n---\n").size(); // Set the rest of the message here as system, etc

        return tokenCount;
    }
}
