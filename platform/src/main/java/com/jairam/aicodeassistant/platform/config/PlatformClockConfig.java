package com.jairam.aicodeassistant.platform.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Provides a single application {@link Clock} bean. All time-dependent code (timestamps on events,
 * token expiry, audit records) MUST inject this rather than calling {@code Instant.now()} directly,
 * so tests can substitute a fixed clock and time-based logic is deterministic.
 */
@AutoConfiguration
public class PlatformClockConfig {

  @Bean
  @ConditionalOnMissingBean
  public Clock systemClock() {
    return Clock.systemUTC();
  }
}
