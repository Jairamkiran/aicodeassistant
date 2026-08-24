package com.jairam.aicodeassistant.iam.domain;

import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Raised when a presented refresh token is unknown, expired, already used, or revoked. Presenting
 * an already-used/revoked token additionally triggers family revocation in the application layer
 * (reuse detection). Renders as HTTP 401.
 */
public class InvalidRefreshTokenException extends ApplicationException {

  private static final long serialVersionUID = 1L;

  public InvalidRefreshTokenException() {
    super(
        ErrorType.AUTHENTICATION,
        HttpStatus.UNAUTHORIZED,
        "Refresh token is invalid or expired",
        Map.of());
  }
}
