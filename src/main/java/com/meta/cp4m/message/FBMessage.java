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
import com.meta.cp4m.message.Payload.Text;
import java.time.Instant;
import java.util.Objects;

public final class FBMessage implements Message {
  private final Instant timestamp;
  private final Identifier instanceId;
  private final Identifier senderId;
  private final Identifier recipientId;

  private final Payload<?> payload;
  private final Role role;

  public FBMessage(
      Instant timestamp,
      Identifier instanceId,
      Identifier senderId,
      Identifier recipientId,
      String message,
      Role role) {
    this.timestamp = timestamp;
    this.instanceId = instanceId;
    this.senderId = senderId;
    this.recipientId = recipientId;
    this.payload = new Text(message);
    this.role = role;
  }

  public FBMessage(
      Instant timestamp,
      Identifier instanceId,
      Identifier senderId,
      Identifier recipientId,
      Payload<?> payload,
      Role role) {
    this.timestamp = timestamp;
    this.instanceId = instanceId;
    this.senderId = senderId;
    this.recipientId = recipientId;
    this.payload = payload;
    this.role = role;
  }

  @Override
  public Instant timestamp() {
    return timestamp;
  }

  @Override
  public Identifier instanceId() {
    return instanceId;
  }

  @Override
  public Identifier senderId() {
    return senderId;
  }

  @Override
  public Identifier recipientId() {
    return recipientId;
  }

  @Override
  public String message() {
    Preconditions.checkState(payload instanceof Text);
    return ((Text) payload).value();
  }

  public Payload<?> payload() {
    return payload;
  }

  @Override
  public Role role() {
    return role;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (FBMessage) obj;
    return Objects.equals(this.timestamp, that.timestamp)
        && Objects.equals(this.instanceId, that.instanceId)
        && Objects.equals(this.senderId, that.senderId)
        && Objects.equals(this.recipientId, that.recipientId)
        && Objects.equals(this.payload, that.payload)
        && Objects.equals(this.role, that.role);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, instanceId, senderId, recipientId, payload, role);
  }

  @Override
  public String toString() {
    return "FBMessage{"
        + "timestamp="
        + timestamp
        + ", instanceId="
        + instanceId
        + ", senderId="
        + senderId
        + ", recipientId="
        + recipientId
        + ", payload="
        + payload
        + ", role="
        + role
        + '}';
  }
}
