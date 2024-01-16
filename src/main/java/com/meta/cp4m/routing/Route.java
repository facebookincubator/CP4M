/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.routing;

import io.javalin.http.HandlerType;

public record Route<IN>(
    String path, HandlerType handlerType, Acceptor<IN> acceptor, Handler<IN> handler) {}
