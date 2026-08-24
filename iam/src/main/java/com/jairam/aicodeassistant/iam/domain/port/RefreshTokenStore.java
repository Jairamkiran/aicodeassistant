package com.jairam.aicodeassistant.iam.domain.port;

import com.jairam.aicodeassistant.iam.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for refresh-token persistence and family operations.
 *
 * <p>Backed by Postgres in M1 (see ADR — Redis is introduced in M2 for rate limiting/locking; this
 * port lets the store be swapped without touching the application layer).
 */
public interface RefreshTokenStore {

  RefreshToken save(RefreshToken token);

  /** Looks up a token by its hash (the value presented by the client, hashed). */
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * Revokes every still-active token in a family — the reuse-detection response to a replayed
   * token. Returns the number of tokens revoked.
   */
  int revokeFamily(UUID familyId, java.time.Instant now);

  /**
   * Deletes tokens that expired before {@code cutoff}. Housekeeping for the scheduled cleanup job;
   * returns the number of rows removed.
   */
  int deleteExpiredBefore(java.time.Instant cutoff);
}
