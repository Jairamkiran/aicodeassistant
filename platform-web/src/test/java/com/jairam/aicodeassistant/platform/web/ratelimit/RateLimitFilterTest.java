package com.jairam.aicodeassistant.platform.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit test for {@link RateLimitFilter} using an in-memory limiter — no Redis, no Spring context.
 * Verifies allow → 200/chain-invoked, deny → 429 + Retry-After + problem body, and that actuator
 * paths are exempt.
 */
class RateLimitFilterTest {

  private RateLimitFilter filter;
  private ConfigurableLimiter limiter;

  /** Deterministic limiter: allows the first {@code allowance} calls, then denies. */
  static final class ConfigurableLimiter implements RateLimiter {
    private final AtomicInteger allowance;

    ConfigurableLimiter(int allowance) {
      this.allowance = new AtomicInteger(allowance);
    }

    @Override
    public Decision tryConsume(String key, long capacity, Duration refillPeriod) {
      if (allowance.getAndDecrement() > 0) {
        return Decision.allowed(Math.max(0, allowance.get()));
      }
      return Decision.denied(Duration.ofSeconds(5));
    }
  }

  @BeforeEach
  void setUp() {
    limiter = new ConfigurableLimiter(2);
    var props = new RateLimitProperties(true, 2, 2, Duration.ofMinutes(1));
    Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    filter = new RateLimitFilter(limiter, props, event -> {}, new ObjectMapper(), clock);
  }

  @Test
  void allowsUntilLimitThenReturns429() throws Exception {
    // First two requests pass.
    for (int i = 0; i < 2; i++) {
      var req = apiRequest();
      var res = new MockHttpServletResponse();
      var chain = new MockFilterChain();
      filter.doFilter(req, res, chain);
      assertThat(res.getStatus()).isEqualTo(200);
      assertThat(chain.getRequest()).as("chain invoked when allowed").isNotNull();
    }

    // Third is rate-limited.
    var req = apiRequest();
    var res = new MockHttpServletResponse();
    var notInvoked = new AtomicInteger(0);
    FilterChain chain = (rq, rs) -> notInvoked.incrementAndGet();
    filter.doFilter(req, res, chain);

    assertThat(res.getStatus()).isEqualTo(429);
    assertThat(res.getHeader("Retry-After")).isEqualTo("5");
    assertThat(res.getContentType()).contains("application/problem+json");
    assertThat(res.getContentAsString()).contains("rate-limited");
    assertThat(notInvoked.get()).as("chain NOT invoked when limited").isZero();
  }

  @Test
  void actuatorPathsAreExempt() throws Exception {
    var req = new MockHttpServletRequest("GET", "/actuator/health");
    req.setRequestURI("/actuator/health");
    var res = new MockHttpServletResponse();
    var chain = new MockFilterChain();
    // Even with allowance exhausted, exempt paths pass through.
    new RateLimitFilter(
            new ConfigurableLimiter(0),
            new RateLimitProperties(true, 1, 1, Duration.ofMinutes(1)),
            event -> {},
            new ObjectMapper(),
            Clock.systemUTC())
        .doFilter(req, res, chain);
    assertThat(res.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
  }

  private static MockHttpServletRequest apiRequest() {
    var req = new MockHttpServletRequest("GET", "/api/v1/users/me");
    req.setRequestURI("/api/v1/users/me");
    req.setRemoteAddr("203.0.113.7");
    return req;
  }
}
