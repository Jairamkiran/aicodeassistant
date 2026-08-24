package com.jairam.aicodeassistant.iam.adapter.rest;

import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/**
 * Extracts the authenticated user's id from the security context, regardless of how the request
 * authenticated.
 *
 * <p>Both auth mechanisms expose the user id as {@link Authentication#getName()}: the JWT bearer
 * path sets it from the {@code sub} claim; the API-key path sets it from the key's owner. Reading
 * {@code getName()} therefore works uniformly and keeps controllers free of Spring Security
 * principal types.
 */
final class CurrentUser {

  private CurrentUser() {}

  static UUID id(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotAuthenticatedException();
    }
    try {
      return UUID.fromString(authentication.getName());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new NotAuthenticatedException();
    }
  }

  /** Raised when the principal is missing or malformed. Renders as HTTP 401. */
  static final class NotAuthenticatedException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    NotAuthenticatedException() {
      super(ErrorType.AUTHENTICATION, HttpStatus.UNAUTHORIZED, "Not authenticated", Map.of());
    }
  }
}
