package com.jairam.aicodeassistant.iam.domain;

import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Raised when authentication fails (bad email/password, or disabled account).
 *
 * <p>The message is intentionally generic — it never reveals whether the email exists or the
 * account is disabled — to avoid user enumeration. Renders as HTTP 401.
 */
public class InvalidCredentialsException extends ApplicationException {

  private static final long serialVersionUID = 1L;

  public InvalidCredentialsException() {
    super(ErrorType.AUTHENTICATION, HttpStatus.UNAUTHORIZED, "Invalid email or password", Map.of());
  }
}
