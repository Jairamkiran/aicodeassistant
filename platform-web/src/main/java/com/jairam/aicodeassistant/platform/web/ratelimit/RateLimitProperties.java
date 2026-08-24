package com.jairam.aicodeassistant.platform.web.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate-limit configuration, bound from {@code aicodeassistant.ratelimit}.
 *
 * <p>Two tiers: a higher allowance for authenticated principals (user or API key) and a stricter
 * one for anonymous/IP-keyed traffic. {@code enabled=false} wires the {@link NoOpRateLimiter}.
 *
 * @param enabled master switch
 * @param authenticatedCapacity burst size for authenticated principals
 * @param anonymousCapacity burst size for anonymous (IP-keyed) callers
 * @param refillPeriod window over which a full capacity refills
 */
@ConfigurationProperties(prefix = "aicodeassistant.ratelimit")
public record RateLimitProperties(
    boolean enabled, long authenticatedCapacity, long anonymousCapacity, Duration refillPeriod) {

  public RateLimitProperties {
    if (authenticatedCapacity <= 0) {
      authenticatedCapacity = 120; // ~120 req/min per user by default
    }
    if (anonymousCapacity <= 0) {
      anonymousCapacity = 30; // stricter for unauthenticated callers
    }
    if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
      refillPeriod = Duration.ofMinutes(1);
    }
  }
}
