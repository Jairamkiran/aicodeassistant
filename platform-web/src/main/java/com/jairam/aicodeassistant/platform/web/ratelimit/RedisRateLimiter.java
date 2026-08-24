package com.jairam.aicodeassistant.platform.web.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis-backed token-bucket rate limiter.
 *
 * <p>The check-refill-consume sequence runs as a single atomic Lua script on the Redis server, so
 * concurrent requests across all app instances cannot race — <em>without</em> any distributed lock.
 * Each bucket is one Redis hash storing {@code tokens} and {@code last_refill}; the script lazily
 * refills based on elapsed time, consumes a token if available, and sets a TTL so idle buckets
 * expire on their own.
 *
 * <p><b>Fail-open:</b> if Redis is unreachable the limiter allows the request (logged at WARN).
 * Availability of the API is prioritised over strict limiting during a Redis outage — see ADR-0005.
 */
public class RedisRateLimiter implements RateLimiter {

  private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

  /**
   * KEYS[1] = bucket key. ARGV: capacity, refillMillis, nowMillis. Returns {allowed(1/0),
   * remaining, retryAfterMillis}.
   */
  private static final String LUA =
      """
      local key = KEYS[1]
      local capacity = tonumber(ARGV[1])
      local refill_ms = tonumber(ARGV[2])
      local now = tonumber(ARGV[3])
      local bucket = redis.call('HMGET', key, 'tokens', 'ts')
      local tokens = tonumber(bucket[1])
      local ts = tonumber(bucket[2])
      if tokens == nil then
        tokens = capacity
        ts = now
      end
      -- Lazy refill proportional to elapsed time.
      local elapsed = math.max(0, now - ts)
      local refilled = elapsed * capacity / refill_ms
      tokens = math.min(capacity, tokens + refilled)
      ts = now
      local allowed = 0
      local retry_after = 0
      if tokens >= 1 then
        tokens = tokens - 1
        allowed = 1
      else
        -- ms until one token refills
        retry_after = math.ceil((1 - tokens) * refill_ms / capacity)
      end
      redis.call('HSET', key, 'tokens', tokens, 'ts', ts)
      redis.call('PEXPIRE', key, refill_ms * 2)
      return { allowed, math.floor(tokens), retry_after }
      """;

  private final StringRedisTemplate redis;
  private final Clock clock;

  // Redis returns a multi-bulk reply mapped to a List; the Spring
  // DefaultRedisScript result type is inherently the raw List class (there is no
  // element-typed overload), so the raw type here is unavoidable, not a smell.
  @SuppressWarnings("rawtypes")
  private final DefaultRedisScript<List> script;

  @SuppressWarnings("rawtypes")
  public RedisRateLimiter(StringRedisTemplate redis, Clock clock) {
    this.redis = redis;
    this.clock = clock;
    this.script = new DefaultRedisScript<>();
    this.script.setScriptText(LUA);
    this.script.setResultType(List.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Decision tryConsume(String key, long capacity, Duration refillPeriod) {
    long now = clock.millis();
    try {
      List<Long> result =
          redis.execute(
              script,
              List.of("ratelimit:" + key),
              Long.toString(capacity),
              Long.toString(refillPeriod.toMillis()),
              Long.toString(now));
      if (result == null || result.size() < 3) {
        return Decision.allowed(capacity); // defensive fail-open
      }
      boolean allowed = result.get(0) == 1L;
      long remaining = result.get(1);
      return allowed
          ? Decision.allowed(remaining)
          : Decision.denied(Duration.ofMillis(result.get(2)));
    } catch (RuntimeException e) {
      // Redis down/unreachable: fail OPEN so the API stays available.
      log.warn("Rate limiter unavailable (failing open) for key {}: {}", key, e.getMessage());
      return Decision.allowed(capacity);
    }
  }
}
