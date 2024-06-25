/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import io.javalin.http.Context;
import java.util.List;

@FunctionalInterface
public interface RequestProcessor<IN, OUT extends Message> {

  List<OUT> process(Context ctx, IN in);
}
