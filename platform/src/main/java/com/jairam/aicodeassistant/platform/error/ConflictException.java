package com.jairam.aicodeassistant.platform.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Thrown when an operation conflicts with current state (e.g. duplicate). Renders as HTTP 409. */
public class ConflictException extends ApplicationException {

  private static final long serialVersionUID = 1L;

  public ConflictException(String message) {
    super(ErrorType.CONFLICT, HttpStatus.CONFLICT, message, Map.of());
  }

  public ConflictException(String message, Map<String, Object> properties) {
    super(ErrorType.CONFLICT, HttpStatus.CONFLICT, message, properties);
  }
}
