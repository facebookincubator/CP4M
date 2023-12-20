/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteRegistrar {
  private final Map<String, List<Route<?>>> routeAcceptors = new HashMap<>();

  void register(Route<?> route) {
    routeAcceptors.computeIfAbsent(route.path(), ignored -> new ArrayList<>()).add(route);
  }
}
