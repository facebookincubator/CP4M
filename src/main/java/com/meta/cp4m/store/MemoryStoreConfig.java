/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.meta.cp4m.message.Message;
import java.util.Objects;
import java.util.UUID;

public class MemoryStoreConfig implements StoreConfig {

  private final String name;
  private final long storageDurationHours;
  private final long storageCapacityMb;
  private final int messageHistoryLength;

  @JsonCreator
  private MemoryStoreConfig(
      @JsonProperty("name") String name,
      @JsonProperty("storage_duration_hours") long storageDurationHours,
      @JsonProperty("storage_capacity_mbs") long storageCapacityMbs,
      @JsonProperty("message_history_length") Integer messageHistoryLength) {
    messageHistoryLength = messageHistoryLength == null ? Integer.MAX_VALUE : messageHistoryLength;
    Preconditions.checkArgument(
        messageHistoryLength > 0, "message_history_length must be greater than zero");
    Preconditions.checkArgument(name != null && !name.isBlank(), "name cannot be blank");
    Preconditions.checkArgument(
        storageDurationHours > 0, "storage_duration_hours must be greater than zero");
    Preconditions.checkArgument(
        storageCapacityMbs > 0, "storage_duration_hours must be greater than zero");

    this.name = Objects.requireNonNull(name);
    this.storageDurationHours = storageDurationHours;
    this.storageCapacityMb = storageCapacityMbs;
    this.messageHistoryLength = messageHistoryLength;
  }

  public static MemoryStoreConfig of(long storageDurationHours, long storageCapacityMb) {
    // readability of the name doesn't matter unless it comes from the config
    return new MemoryStoreConfig(
        UUID.randomUUID().toString(), storageDurationHours, storageCapacityMb, Integer.MAX_VALUE);
  }

  public static MemoryStoreConfig of(
      long storageDurationHours, long storageCapacityMb, int messageHistoryLength) {
    // readability of the name doesn't matter unless it comes from the config
    return new MemoryStoreConfig(
        UUID.randomUUID().toString(),
        storageDurationHours,
        storageCapacityMb,
        messageHistoryLength);
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

  public int messageHistoryLength() {
    return messageHistoryLength;
  }
}
