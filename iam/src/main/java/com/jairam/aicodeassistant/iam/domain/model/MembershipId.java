package com.jairam.aicodeassistant.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Strongly-typed identifier for a {@link Membership}. */
public record MembershipId(UUID value) {

  public MembershipId {
    Objects.requireNonNull(value, "MembershipId value must not be null");
  }

  public static MembershipId newId() {
    return new MembershipId(UUID.randomUUID());
  }

  public static MembershipId of(String value) {
    return new MembershipId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
