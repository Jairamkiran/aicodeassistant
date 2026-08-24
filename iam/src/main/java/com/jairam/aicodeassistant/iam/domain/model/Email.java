package com.jairam.aicodeassistant.iam.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Email value object. Normalises to lower-case and trims whitespace, and enforces a pragmatic
 * syntactic check at construction so an invalid email can never exist inside the domain.
 *
 * <p>The regex is deliberately conservative (not full RFC 5322) — it rejects the
 * obviously-malformed while accepting real-world addresses; definitive validation is delivery, not
 * a regex.
 */
public record Email(String value) {

  private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private static final int MAX_LENGTH = 254; // RFC 5321 practical maximum.

  public Email {
    Objects.requireNonNull(value, "Email must not be null");
    value = value.trim().toLowerCase(java.util.Locale.ROOT);
    if (value.isEmpty() || value.length() > MAX_LENGTH || !PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid email address");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
