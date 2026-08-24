package com.jairam.aicodeassistant.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Strongly-typed identifier for an {@link Organization}. */
public record OrganizationId(UUID value) {

  public OrganizationId {
    Objects.requireNonNull(value, "OrganizationId value must not be null");
  }

  public static OrganizationId newId() {
    return new OrganizationId(UUID.randomUUID());
  }

  public static OrganizationId of(String value) {
    return new OrganizationId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
