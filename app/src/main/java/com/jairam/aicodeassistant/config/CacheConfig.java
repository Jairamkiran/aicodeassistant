package com.jairam.aicodeassistant.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables caching for the monolith and provides an in-process Caffeine {@link CacheManager}.
 *
 * <p>Caffeine (not Redis) is deliberate for the hot authorization read ({@code org-membership},
 * fired on every authorized request): an in-JVM lookup avoids a network round-trip on the request
 * path and cannot fail the request if a cache backend is down. Entries are short-lived (seconds)
 * and the cache is small; membership mutations evict the affected entry immediately, so staleness
 * is bounded and correctness preserved. Redis remains the right tool for cross-instance state such
 * as rate limiting — this is a per-instance read cache.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

  /** Default caffeine spec: expire-after-write with a modest maximum size. */
  @Bean
  CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setCaffeine(
        Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofSeconds(60)));
    return manager;
  }
}
