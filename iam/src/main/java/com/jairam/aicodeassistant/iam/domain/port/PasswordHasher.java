package com.jairam.aicodeassistant.iam.domain.port;

/**
 * Outbound port for password hashing/verification.
 *
 * <p>Keeps cryptography out of the domain: the {@link
 * com.jairam.aicodeassistant.iam.domain.model.User} aggregate only ever holds a hash produced by an
 * adapter (BCrypt in M1, via a {@code DelegatingPasswordEncoder} so the scheme can be upgraded
 * later).
 */
public interface PasswordHasher {

  /** Hashes a raw password for storage. */
  String hash(String rawPassword);

  /** Verifies a raw password against a stored hash in constant time. */
  boolean matches(String rawPassword, String storedHash);
}
