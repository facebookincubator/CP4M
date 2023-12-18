/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.meta.cp4m.routing.Acceptor;
import io.javalin.http.HandlerType;

public record RequestHandler<IN, OUT extends Message>(
    HandlerType type, Acceptor<IN> acceptor, RequestProcessor<IN, OUT> processor) {}
