package com.jairam.aicodeassistant.platform.observability;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * Access to the current request's correlation id.
 *
 * <p>The value is stored in SLF4J's {@link MDC} under {@link #MDC_KEY} so it is automatically
 * attached to every log line (the logback pattern includes it) and is retrievable anywhere in the
 * call stack without threading a parameter. The {@link CorrelationIdFilter} populates and clears it
 * per request.
 */
public final class CorrelationId {

  /** MDC key; also the log field name and the inbound/outbound HTTP header base. */
  public static final String MDC_KEY = "correlationId";

  /** HTTP header clients may send to propagate an existing correlation id. */
  public static final String HEADER = "X-Correlation-Id";

  private CorrelationId() {}

  /** Returns the current correlation id, or a sentinel if none is bound. */
  public static String current() {
    String value = MDC.get(MDC_KEY);
    return value == null ? "no-correlation-id" : value;
  }

  /**
   * Binds a correlation id to the current thread's MDC. If {@code value} is null/blank a fresh UUID
   * is generated. Returns the resolved value. Called by the correlation-id filter (in {@code
   * platform-web}) at request entry.
   */
  public static String bind(String value) {
    String resolved = (value == null || value.isBlank()) ? generate() : value.trim();
    MDC.put(MDC_KEY, resolved);
    return resolved;
  }

  /** Clears the bound correlation id; must be called in a finally block. */
  public static void clear() {
    MDC.remove(MDC_KEY);
  }

  /** Generates a fresh correlation id value. */
  public static String generate() {
    return UUID.randomUUID().toString();
  }
}
