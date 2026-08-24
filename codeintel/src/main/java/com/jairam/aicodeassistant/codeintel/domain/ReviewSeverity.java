package com.jairam.aicodeassistant.codeintel.domain;

import java.util.Locale;

/** Severity of a code-review finding, ordered most to least serious. */
public enum ReviewSeverity {
  CRITICAL,
  HIGH,
  MEDIUM,
  LOW,
  INFO;

  /** Lenient parse from model output; unknown/blank values map to {@link #INFO}. */
  public static ReviewSeverity fromString(String value) {
    if (value == null || value.isBlank()) {
      return INFO;
    }
    try {
      return ReviewSeverity.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return INFO;
    }
  }
}
