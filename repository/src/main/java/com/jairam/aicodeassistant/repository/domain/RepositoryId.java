package com.jairam.aicodeassistant.repository.domain;

import java.util.Objects;
import java.util.UUID;

/** Strongly-typed identifier for a {@link Repository}. */
public record RepositoryId(UUID value) {

  public RepositoryId {
    Objects.requireNonNull(value, "RepositoryId value must not be null");
  }

  public static RepositoryId newId() {
    return new RepositoryId(UUID.randomUUID());
  }

  public static RepositoryId of(String value) {
    return new RepositoryId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
