package com.jairam.aicodeassistant.platform.web.ratelimit;

import java.time.Duration;

/**
 * A rate limiter over a keyed token bucket.
 *
 * <p>Kept as an interface so the servlet filter is independent of the backing store: the production
 * implementation is Redis-backed (atomic, shared across instances); a no-op implementation is used
 * when Redis is not configured, and a simple in-memory one is used in unit tests.
 */
public interface RateLimiter {

  /**
   * Attempts to consume one token from the bucket identified by {@code key}.
   *
   * @param key bucket identity (e.g. {@code "user:<id>"} or {@code "ip:<addr>"})
   * @param capacity maximum tokens (burst size)
   * @param refillPeriod time over which a full {@code capacity} of tokens refills
   * @return the decision: allowed/denied plus metadata for response headers
   */
  Decision tryConsume(String key, long capacity, Duration refillPeriod);

  /**
   * Outcome of a rate-limit check.
   *
   * @param allowed whether the request may proceed
   * @param remaining tokens left in the bucket after this check
   * @param retryAfter when denied, how long to wait before retrying; else zero
   */
  record Decision(boolean allowed, long remaining, Duration retryAfter) {

    static Decision allowed(long remaining) {
      return new Decision(true, remaining, Duration.ZERO);
    }

    static Decision denied(Duration retryAfter) {
      return new Decision(false, 0, retryAfter);
    }
  }
}
