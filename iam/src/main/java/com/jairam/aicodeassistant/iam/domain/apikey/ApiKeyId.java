package com.jairam.aicodeassistant.iam.domain.apikey;

import java.util.Objects;
import java.util.UUID;

/** Strongly-typed identifier for an {@link ApiKey}. */
public record ApiKeyId(UUID value) {

  public ApiKeyId {
    Objects.requireNonNull(value, "ApiKeyId value must not be null");
  }

  public static ApiKeyId newId() {
    return new ApiKeyId(UUID.randomUUID());
  }

  public static ApiKeyId of(String value) {
    return new ApiKeyId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
