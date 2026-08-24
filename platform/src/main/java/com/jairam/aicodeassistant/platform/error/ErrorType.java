package com.jairam.aicodeassistant.platform.error;

/**
 * Enumeration of stable, machine-readable error categories surfaced by the API.
 *
 * <p>Each value maps to a URN used as the {@code type} field of an RFC-9457 {@link
 * org.springframework.http.ProblemDetail}. Clients and dashboards can switch on these without
 * parsing human-readable messages, and the URN is stable across releases (the human {@code
 * title}/{@code detail} may change).
 */
public enum ErrorType {
  VALIDATION("validation-error", "Request validation failed"),
  AUTHENTICATION("authentication-error", "Authentication is required or failed"),
  AUTHORIZATION("authorization-error", "You are not permitted to perform this action"),
  NOT_FOUND("resource-not-found", "The requested resource was not found"),
  CONFLICT("resource-conflict", "The request conflicts with the current state"),
  RATE_LIMITED("rate-limited", "Too many requests"),
  DEPENDENCY_UNAVAILABLE("dependency-unavailable", "A downstream dependency is unavailable"),
  INTERNAL("internal-error", "An unexpected error occurred");

  private static final String BASE_URN = "https://aicodeassistant.dev/problems/";

  private final String slug;
  private final String defaultTitle;

  ErrorType(String slug, String defaultTitle) {
    this.slug = slug;
    this.defaultTitle = defaultTitle;
  }

  /** The absolute URN placed in {@code ProblemDetail.type}. */
  public String type() {
    return BASE_URN + slug;
  }

  /** Human-readable default title; may be overridden per-occurrence. */
  public String defaultTitle() {
    return defaultTitle;
  }

  /** Stable short code (also echoed in an extension field for easy filtering). */
  public String code() {
    return slug;
  }
}
