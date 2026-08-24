package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.domain.port.RefreshTokenStore;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled housekeeping that removes refresh tokens which have been expired long enough to no
 * longer be needed for reuse detection. Expired tokens are already rejected at authentication time;
 * this simply keeps the table from growing unbounded.
 *
 * <p>Runs on a fixed delay (default hourly). In a multi-instance deployment every instance would
 * run it, which is harmless here: the delete is idempotent and set-based, so overlapping runs just
 * remove nothing on the second pass. A distributed scheduler is intentionally not introduced for a
 * cheap idempotent cleanup (no speculative infrastructure).
 */
@Component
class RefreshTokenMaintenance {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokenMaintenance.class);

  private final RefreshTokenStore tokens;
  private final Clock clock;

  RefreshTokenMaintenance(RefreshTokenStore tokens, Clock clock) {
    this.tokens = tokens;
    this.clock = clock;
  }

  @Scheduled(
      initialDelayString = "${aicodeassistant.iam.token-cleanup.initial-delay-ms:60000}",
      fixedDelayString = "${aicodeassistant.iam.token-cleanup.interval-ms:3600000}")
  @Transactional
  void purgeExpiredTokens() {
    int removed = tokens.deleteExpiredBefore(clock.instant());
    if (removed > 0) {
      log.info("Purged {} expired refresh token(s)", removed);
    }
  }
}
