/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.google.common.base.Preconditions;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.reflection.qual.NewInstance;

public class UserData {

  private final @Nullable String name;
  private final @Nullable String phoneNumber;

  private UserData(@Nullable String name, @Nullable String phoneNumber) {
    this.name = name;
    this.phoneNumber = phoneNumber;
  }

  static UserData empty() {
    return new UserData(null, null);
  }

  public Optional<String> name() {
    return Optional.ofNullable(name);
  }

  public @NewInstance UserData withName(String name) {
    Objects.requireNonNull(name);
    Preconditions.checkArgument(!name.isBlank(), "name is empty");
    return new UserData(name.strip(), this.phoneNumber);
  }

  public Optional<String> phoneNumber() {
    return Optional.ofNullable(phoneNumber);
  }

  public @NewInstance UserData withPhoneNumber(String phoneNumber) {
    Objects.requireNonNull(phoneNumber);
    Preconditions.checkArgument(!phoneNumber.isBlank(), "phone number is empty");
    return new UserData(this.name, phoneNumber.strip());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserData userData = (UserData) o;
    return Objects.equals(name, userData.name) && Objects.equals(phoneNumber, userData.phoneNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, phoneNumber);
  }
}
