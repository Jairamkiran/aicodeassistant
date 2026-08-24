package com.jairam.aicodeassistant.platform.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Thrown when a requested aggregate/entity does not exist. Renders as HTTP 404. */
public class ResourceNotFoundException extends ApplicationException {

  private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(String resource, Object identifier) {
    super(
        ErrorType.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        "%s not found: %s".formatted(resource, identifier),
        Map.of("resource", resource, "identifier", String.valueOf(identifier)));
  }

  public ResourceNotFoundException(String message) {
    super(ErrorType.NOT_FOUND, HttpStatus.NOT_FOUND, message, Map.of());
  }
}
