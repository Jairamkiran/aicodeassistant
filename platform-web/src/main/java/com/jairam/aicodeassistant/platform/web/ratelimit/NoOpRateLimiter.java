package com.jairam.aicodeassistant.platform.web.ratelimit;

import java.time.Duration;

/**
 * A limiter that never limits. Wired when rate limiting is disabled or no Redis is configured, so a
 * deployable still boots and serves traffic (fail-open by construction). Keeps the filter code path
 * uniform — it always has a limiter.
 */
public class NoOpRateLimiter implements RateLimiter {

  @Override
  public Decision tryConsume(String key, long capacity, Duration refillPeriod) {
    return Decision.allowed(capacity);
  }
}
