package com.jairam.aicodeassistant.iam.adapter.rest;

import com.jairam.aicodeassistant.iam.config.IamAuthProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the refresh-token cookie consistently for issue and clear operations.
 *
 * <p>The cookie is {@code HttpOnly} (invisible to JavaScript, mitigating XSS token theft), {@code
 * SameSite=Strict} (mitigating CSRF), path-scoped to the auth endpoints (so it is only sent where
 * it is needed), and {@code Secure} in non-local environments (configurable).
 */
@Component
class RefreshCookieFactory {

  private final IamAuthProperties properties;

  RefreshCookieFactory(IamAuthProperties properties) {
    this.properties = properties;
  }

  /** A cookie carrying the refresh secret, valid for the refresh TTL. */
  ResponseCookie issue(String rawRefreshToken, Duration ttl) {
    return base(rawRefreshToken).maxAge(ttl).build();
  }

  /** A cookie that immediately expires the refresh cookie (logout). */
  ResponseCookie clear() {
    return base("").maxAge(0).build();
  }

  private ResponseCookie.ResponseCookieBuilder base(String value) {
    return ResponseCookie.from(properties.refreshCookieName(), value)
        .httpOnly(true)
        .secure(properties.refreshCookieSecure())
        .sameSite("Strict")
        .path(properties.refreshCookiePath());
  }

  String cookieName() {
    return properties.refreshCookieName();
  }
}
