package com.jairam.aicodeassistant.platform.web.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Wires rate limiting for web deployables.
 *
 * <p>Chooses the limiter implementation by configuration and availability: a {@link
 * RedisRateLimiter} when rate limiting is enabled and a {@link StringRedisTemplate} bean exists,
 * otherwise a {@link NoOpRateLimiter} (fail-open). Exposes the {@link RateLimitFilter} as a plain
 * bean; the security configuration inserts it INTO the Spring Security filter chain (after
 * authentication) so the authenticated principal is available and still bound to the {@code
 * SecurityContext} when the limiter keys its bucket. Registering it as a standalone servlet filter
 * would run it after Security has already cleared the context — hence the in-chain placement.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(RateLimiter.class)
  @ConditionalOnClass(StringRedisTemplate.class)
  RateLimiter rateLimiter(
      RateLimitProperties properties,
      org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> redisTemplate,
      Clock clock) {
    StringRedisTemplate template = redisTemplate.getIfAvailable();
    if (properties.enabled() && template != null) {
      return new RedisRateLimiter(template, clock);
    }
    // Disabled, or no Redis configured → allow everything (fail-open).
    return new NoOpRateLimiter();
  }

  @Bean
  RateLimitFilter rateLimitFilter(
      RateLimiter rateLimiter,
      RateLimitProperties properties,
      ApplicationEventPublisher events,
      ObjectMapper objectMapper,
      Clock clock) {
    return new RateLimitFilter(rateLimiter, properties, events, objectMapper, clock);
  }
}
