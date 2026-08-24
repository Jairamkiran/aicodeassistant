package com.jairam.aicodeassistant.iam.domain.port;

/**
 * Outbound port producing opaque refresh-token secrets and their storage hashes.
 *
 * <p>The raw secret is returned to the client exactly once; only its hash is persisted (see {@link
 * RefreshTokenStore}). Separating generation from storage keeps the randomness/hashing choice in an
 * adapter.
 */
public interface RefreshTokenGenerator {

  /** A freshly generated refresh token: the raw secret and its storage hash. */
  record GeneratedRefreshToken(String rawValue, String tokenHash) {}

  /** Generates a cryptographically-random token and its hash. */
  GeneratedRefreshToken generate();

  /** Hashes a client-presented raw token so it can be looked up by hash. */
  String hash(String rawValue);
}
