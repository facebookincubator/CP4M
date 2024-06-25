/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.meta.cp4m.Identifier;
import org.junit.jupiter.api.Test;

class UserDataTest {

  @Test
  void mergeWithValuesFull() {
    Identifier id = Identifier.random();
    UserData userData1 = UserData.create(id).withName("name1").withPhoneNumber("phone1");
    UserData userData2 = UserData.create(id).withName("name2").withPhoneNumber("phone2");
    assertThat(userData1.creationTime()).isBefore(userData2.creationTime());
    UserData merged = UserData.merge(userData1, userData2);
    assertThat(merged)
        .isEqualTo(UserData.merge(userData2, userData1))
        .isEqualTo(userData1.merge(userData2))
        .isEqualTo(userData2.merge(userData1));

    // second values should be used because the second UserData is newer
    assertThat(merged.name()).get().isEqualTo(userData2.name().get());
    assertThat(merged.phoneNumber()).get().isEqualTo(userData2.phoneNumber().get());
  }

  @Test
  void mergeWithFirstValueEmpty() {
    Identifier id = Identifier.random();
    UserData userData1 = UserData.create(id);
    UserData userData2 = UserData.create(id).withName("name2").withPhoneNumber("phone2");
    assertThat(userData1.creationTime()).isBefore(userData2.creationTime());
    UserData merged = UserData.merge(userData1, userData2);
    assertThat(merged)
        .isEqualTo(UserData.merge(userData2, userData1))
        .isEqualTo(userData1.merge(userData2))
        .isEqualTo(userData2.merge(userData1));

    // second values should be used because the second UserData is newer
    assertThat(merged.name()).get().isEqualTo(userData2.name().get());
    assertThat(merged.phoneNumber()).get().isEqualTo(userData2.phoneNumber().get());
  }

  @Test
  void mergeWithSecondValueEmpty() {
    Identifier id = Identifier.random();
    UserData userData1 = UserData.create(id).withName("name2").withPhoneNumber("phone2");
    UserData userData2 = UserData.create(id);
    assertThat(userData1.creationTime()).isBefore(userData2.creationTime());
    UserData merged = UserData.merge(userData1, userData2);
    assertThat(merged)
        .isEqualTo(UserData.merge(userData2, userData1))
        .isEqualTo(userData1.merge(userData2))
        .isEqualTo(userData2.merge(userData1));

    // second values should be used because the second UserData is newer
    assertThat(merged.name()).get().isEqualTo(userData1.name().get());
    assertThat(merged.phoneNumber()).get().isEqualTo(userData1.phoneNumber().get());
  }

  @Test
  void cannotMergeWithDifferentUserIds() {
    Identifier id1 = Identifier.random();
    Identifier id2 = Identifier.random();
    UserData userData1 = UserData.create(id1).withName("name1").withPhoneNumber("phone1");
    UserData userData2 = UserData.create(id2).withName("name2").withPhoneNumber("phone2");
    assertThrows(IllegalArgumentException.class, () -> UserData.merge(userData1, userData2));
  }
}
