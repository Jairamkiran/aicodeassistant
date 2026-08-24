package com.jairam.aicodeassistant.platform.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies the real Redis-backed token-bucket limiter against a live Redis (Testcontainers): the
 * atomic Lua script must allow exactly {@code capacity} requests in a window, then deny — proving
 * the check-refill-consume logic works on the actual server. Requires Docker; runs under {@code
 * ./gradlew integrationTest}.
 */
class RedisRateLimiterIT {

  @SuppressWarnings("resource")
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);

  private static LettuceConnectionFactory connectionFactory;
  private static StringRedisTemplate redisTemplate;
  private RedisRateLimiter limiter;

  @BeforeAll
  static void startRedis() {
    REDIS.start();
    connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();
    redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();
  }

  @AfterAll
  static void stopRedis() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
    REDIS.stop();
  }

  @Test
  void allowsUpToCapacityThenDenies() {
    limiter = new RedisRateLimiter(redisTemplate, Clock.systemUTC());
    String key = "it-" + System.nanoTime();
    Duration window = Duration.ofMinutes(1);

    // Capacity of 3: first three consume succeeds, fourth is denied.
    assertThat(limiter.tryConsume(key, 3, window).allowed()).isTrue();
    assertThat(limiter.tryConsume(key, 3, window).allowed()).isTrue();
    assertThat(limiter.tryConsume(key, 3, window).allowed()).isTrue();

    RateLimiter.Decision denied = limiter.tryConsume(key, 3, window);
    assertThat(denied.allowed()).isFalse();
    assertThat(denied.retryAfter()).isPositive();
  }

  @Test
  void separateKeysHaveSeparateBuckets() {
    limiter = new RedisRateLimiter(redisTemplate, Clock.systemUTC());
    Duration window = Duration.ofMinutes(1);
    String a = "bucket-a-" + System.nanoTime();
    String b = "bucket-b-" + System.nanoTime();

    assertThat(limiter.tryConsume(a, 1, window).allowed()).isTrue();
    assertThat(limiter.tryConsume(a, 1, window).allowed()).isFalse();
    // Different key is unaffected.
    assertThat(limiter.tryConsume(b, 1, window).allowed()).isTrue();
  }
}
