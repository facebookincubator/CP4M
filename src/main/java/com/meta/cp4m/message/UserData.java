/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.google.common.base.Preconditions;
import com.meta.cp4m.Identifier;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.reflection.qual.NewInstance;

public class UserData {

  private final @Nullable String name;
  private final @Nullable String phoneNumber;
  private final Instant creationTime = Instant.now();
  private final Identifier userId;

  private UserData(Identifier userId, @Nullable String name, @Nullable String phoneNumber) {
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.userId = userId;
  }

  static UserData create(Identifier userId) {
    return new UserData(userId, null, null);
  }

  public Optional<String> name() {
    return Optional.ofNullable(name);
  }

  public static UserData merge(UserData u1, UserData u2) {
    Preconditions.checkArgument(Objects.equals(u1.userId, u2.userId));
    UserData newest = u1.creationTime().isAfter(u2.creationTime()) ? u1 : u2;
    UserData oldest = newest == u1 ? u2 : u1;
    return new UserData(
        newest.userId,
        newest.name != null ? newest.name : oldest.name,
        newest.phoneNumber != null ? newest.phoneNumber : oldest.phoneNumber);
  }

  public Optional<String> phoneNumber() {
    return Optional.ofNullable(phoneNumber);
  }

  public @NewInstance UserData withName(String name) {
    Objects.requireNonNull(name);
    Preconditions.checkArgument(!name.isBlank(), "name is empty");
    return new UserData(this.userId, name.strip(), this.phoneNumber);
  }

  public @NewInstance UserData withPhoneNumber(String phoneNumber) {
    Objects.requireNonNull(phoneNumber);
    Preconditions.checkArgument(!phoneNumber.isBlank(), "phone number is empty");
    return new UserData(this.userId, this.name, phoneNumber.strip());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserData userData = (UserData) o;
    return Objects.equals(name, userData.name)
        && Objects.equals(phoneNumber, userData.phoneNumber)
        && Objects.equals(userId, userData.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, phoneNumber, userId);
  }

  Instant creationTime() {
    return creationTime;
  }

  public @NewInstance UserData merge(UserData other) {
    return merge(this, other);
  }
}
