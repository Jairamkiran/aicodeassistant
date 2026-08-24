package com.jairam.aicodeassistant.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for a {@link User}.
 *
 * <p>Wrapping the raw {@link UUID} prevents accidentally passing a user id where an organization id
 * is expected — a class of bug that bare {@code UUID}s allow.
 */
public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "UserId value must not be null");
  }

  /** Generates a new random user id. */
  public static UserId newId() {
    return new UserId(UUID.randomUUID());
  }

  /** Parses a user id from its string form. */
  public static UserId of(String value) {
    return new UserId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
