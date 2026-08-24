package com.jairam.aicodeassistant.iam.adapter.rest.dto;

/**
 * Access-token response body. The refresh token is NOT included here — it is delivered as an
 * HttpOnly cookie so client-side JavaScript cannot read it.
 *
 * @param accessToken signed JWT bearer token
 * @param tokenType always {@code Bearer}
 * @param expiresInSeconds access-token lifetime in seconds
 */
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {

  public static TokenResponse bearer(String accessToken, long expiresInSeconds) {
    return new TokenResponse(accessToken, "Bearer", expiresInSeconds);
  }
}
