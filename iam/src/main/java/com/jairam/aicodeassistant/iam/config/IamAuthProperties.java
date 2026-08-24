package com.jairam.aicodeassistant.iam.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised authentication settings (token lifetimes, JWT signing, issuer).
 *
 * <p>Bound from the {@code aicodeassistant.iam.auth} configuration namespace. Secrets (the JWT
 * signing secret) come from the environment; the development fallback in {@code application.yml}
 * must be overridden in any real deployment.
 *
 * @param accessTokenTtl how long an access token is valid
 * @param refreshTokenTtl how long a refresh token (family) is valid
 * @param issuer the {@code iss} claim placed in access tokens
 * @param jwtSecret HMAC signing secret (min 32 bytes for HS256)
 * @param refreshCookieName name of the HttpOnly refresh cookie
 * @param refreshCookieSecure whether the refresh cookie sets the Secure flag
 * @param refreshCookiePath path scope of the refresh cookie
 */
@ConfigurationProperties(prefix = "aicodeassistant.iam.auth")
public record IamAuthProperties(
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    String issuer,
    String jwtSecret,
    String refreshCookieName,
    boolean refreshCookieSecure,
    String refreshCookiePath) {

  public IamAuthProperties {
    accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
    refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(14) : refreshTokenTtl;
    issuer = (issuer == null || issuer.isBlank()) ? "aicodeassistant" : issuer;
    refreshCookieName =
        (refreshCookieName == null || refreshCookieName.isBlank())
            ? "aicodeassistant_refresh"
            : refreshCookieName;
    refreshCookiePath =
        (refreshCookiePath == null || refreshCookiePath.isBlank())
            ? "/api/v1/auth"
            : refreshCookiePath;
  }
}
