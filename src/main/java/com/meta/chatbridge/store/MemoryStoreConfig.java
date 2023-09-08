/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.chatbridge.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.meta.chatbridge.message.Message;
import java.util.Objects;
import java.util.UUID;

public class MemoryStoreConfig implements StoreConfig {

  private final String name;
  private final long storageDurationHours;
  private final long storageCapacityMb;

  @JsonCreator
  private MemoryStoreConfig(
      @JsonProperty("name") String name,
      @JsonProperty("storage_duration_hours") long storageDurationHours,
      @JsonProperty("storage_capacity_mbs") long storageCapacityMbs) {
    Preconditions.checkArgument(name != null && !name.isBlank(), "name cannot be blank");
    Preconditions.checkArgument(
        storageDurationHours > 0, "storage_duration_hours must be greater than zero");
    Preconditions.checkArgument(
        storageCapacityMbs > 0, "storage_duration_hours must be greater than zero");

    this.name = Objects.requireNonNull(name);
    this.storageDurationHours = storageDurationHours;
    this.storageCapacityMb = storageCapacityMbs;
  }

  public static MemoryStoreConfig of(long storageDurationHours, long storageCapacityMb) {
    // readability of the name doesn't matter unless it comes from the config
    return new MemoryStoreConfig(
        UUID.randomUUID().toString(), storageDurationHours, storageCapacityMb);
  }

  @Override
  public String name() {
    return name;
  }

  public long storageDurationHours() {
    return storageDurationHours;
  }

  public long storageCapacityMb() {
    return storageCapacityMb;
  }

  @Override
  public <T extends Message> MemoryStore<T> toStore() {
    return new MemoryStore<>(this);
  }
}
