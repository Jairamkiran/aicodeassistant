package com.jairam.aicodeassistant.platform.web;

import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Registers cross-cutting web infrastructure that every web-facing deployable inherits: the
 * correlation-id filter (highest precedence) and the central RFC-9457 exception handler.
 *
 * <p>Lives in {@code platform-web} so only deployables that depend on it (the {@code app}) pick it
 * up; the non-web indexer-worker never sees these beans. Guarded by {@link
 * ConditionalOnWebApplication} for defence in depth.
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class PlatformWebAutoConfiguration {

  @Bean
  public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
    FilterRegistrationBean<CorrelationIdFilter> registration =
        new FilterRegistrationBean<>(new CorrelationIdFilter());
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");
    return registration;
  }

  @Bean
  public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
    FilterRegistrationBean<SecurityHeadersFilter> registration =
        new FilterRegistrationBean<>(new SecurityHeadersFilter());
    // Just after the correlation-id filter, before security — headers apply to
    // every response including error responses.
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    registration.addUrlPatterns("/*");
    return registration;
  }

  @Bean
  public GlobalExceptionHandler globalExceptionHandler(Clock clock) {
    return new GlobalExceptionHandler(clock);
  }
}
