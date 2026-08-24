package com.jairam.aicodeassistant.platform.web.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairam.aicodeassistant.platform.audit.AuditSignal;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import com.jairam.aicodeassistant.platform.observability.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces per-principal rate limits.
 *
 * <p>Runs after authentication (so it can key by user/API-key when present, and fall back to client
 * IP otherwise) but before controllers. On breach it returns {@code 429} with a {@code Retry-After}
 * header and an RFC-9457 problem body, and publishes a {@link RateLimitExceededEvent} for the audit
 * log. Actuator and docs paths are exempt.
 *
 * <p>The limiter itself fails open on Redis outage (see {@link RedisRateLimiter}), so this filter
 * never takes the API down when the limiter's backend is unhealthy.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private final RateLimiter rateLimiter;
  private final RateLimitProperties properties;
  private final ApplicationEventPublisher events;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public RateLimitFilter(
      RateLimiter rateLimiter,
      RateLimitProperties properties,
      ApplicationEventPublisher events,
      ObjectMapper objectMapper,
      Clock clock) {
    this.rateLimiter = rateLimiter;
    this.properties = properties;
    this.events = events;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/swagger-ui");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean authenticated = auth != null && auth.isAuthenticated() && auth.getName() != null;

    String key = authenticated ? "user:" + auth.getName() : "ip:" + clientIp(request);
    long capacity =
        authenticated ? properties.authenticatedCapacity() : properties.anonymousCapacity();

    RateLimiter.Decision decision =
        rateLimiter.tryConsume(key, capacity, properties.refillPeriod());

    response.setHeader("X-RateLimit-Limit", Long.toString(capacity));
    response.setHeader("X-RateLimit-Remaining", Long.toString(Math.max(0, decision.remaining())));

    if (decision.allowed()) {
      filterChain.doFilter(request, response);
      return;
    }

    writeTooManyRequests(request, response, decision.retryAfter());
    events.publishEvent(
        AuditSignal.builder("RATE_LIMIT_EXCEEDED")
            .success(false)
            .actor(authenticated ? "USER" : "ANONYMOUS", authenticated ? auth.getName() : null)
            .target("ENDPOINT", request.getMethod() + " " + request.getRequestURI())
            .occurredAt(clock.instant())
            .attributes(java.util.Map.of("bucket", key, "clientIp", clientIp(request)))
            .build());
  }

  private void writeTooManyRequests(
      HttpServletRequest request, HttpServletResponse response, Duration retryAfter)
      throws IOException {
    long retrySeconds = Math.max(1, retryAfter.toSeconds());
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retrySeconds));

    // RFC-9457 body, matching the shape produced by GlobalExceptionHandler.
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", ErrorType.RATE_LIMITED.type());
    body.put("title", ErrorType.RATE_LIMITED.defaultTitle());
    body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
    body.put("detail", "Rate limit exceeded. Retry after " + retrySeconds + "s.");
    body.put("code", ErrorType.RATE_LIMITED.code());
    body.put("correlationId", CorrelationId.current());
    body.put("timestamp", OffsetDateTime.now(clock).toString());
    body.put("instance", URI.create(request.getRequestURI()).toString());
    objectMapper.writeValue(response.getOutputStream(), body);
  }

  /** Best-effort client IP: honour X-Forwarded-For's first hop, else the socket. */
  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
