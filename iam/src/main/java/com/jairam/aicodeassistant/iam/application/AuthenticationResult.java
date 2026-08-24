package com.jairam.aicodeassistant.iam.application;

import java.time.Duration;

/**
 * Result of a successful login or token refresh.
 *
 * <p>Carries the signed access token (for the response body) and the raw refresh token secret
 * (which the REST adapter places in an HttpOnly cookie — it is never logged or returned in the
 * body). The raw refresh value exists only in memory for the duration of the response; only its
 * hash is persisted.
 *
 * @param accessToken signed JWT access token
 * @param accessTokenTtl access-token validity (for the client)
 * @param rawRefreshToken opaque refresh secret to set as an HttpOnly cookie
 * @param refreshTokenTtl refresh-token validity (cookie max-age)
 */
public record AuthenticationResult(
    String accessToken,
    Duration accessTokenTtl,
    String rawRefreshToken,
    Duration refreshTokenTtl) {}
