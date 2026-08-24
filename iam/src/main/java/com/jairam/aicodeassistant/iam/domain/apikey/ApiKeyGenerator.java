package com.jairam.aicodeassistant.iam.domain.apikey;

/**
 * Outbound port producing new API-key material and verifying presented keys.
 *
 * <p>A generated key is {@code aca_<prefix>.<secret>}: the prefix is stored in clear (public
 * identifier), the secret only as a hash. The full raw key is returned to the caller exactly once.
 */
public interface ApiKeyGenerator {

  /** A newly generated key: the one-time raw value plus what to persist. */
  record GeneratedApiKey(String rawKey, String keyPrefix, String secretHash) {}

  /** Generates a new key (prefix + secret + hash). */
  GeneratedApiKey generate();

  /**
   * Splits a presented raw key into its prefix and secret parts.
   *
   * @return the parsed parts, or empty if the key is malformed
   */
  java.util.Optional<ParsedApiKey> parse(String rawKey);

  /** Verifies a presented secret against a stored hash (constant time). */
  boolean matches(String presentedSecret, String storedHash);

  /** The prefix and secret extracted from a presented raw key. */
  record ParsedApiKey(String keyPrefix, String secret) {}
}
