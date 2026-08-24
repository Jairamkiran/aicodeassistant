package com.jairam.aicodeassistant.platform.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Thrown for domain-level validation failures that are not caught by bean validation on the request
 * DTO (e.g. cross-field or stateful invariants). Renders as HTTP 400.
 */
public class ValidationException extends ApplicationException {

  private static final long serialVersionUID = 1L;

  public ValidationException(String message) {
    super(ErrorType.VALIDATION, HttpStatus.BAD_REQUEST, message, Map.of());
  }

  public ValidationException(String message, Map<String, Object> properties) {
    super(ErrorType.VALIDATION, HttpStatus.BAD_REQUEST, message, properties);
  }
}
