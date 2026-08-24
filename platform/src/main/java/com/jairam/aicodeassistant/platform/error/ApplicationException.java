package com.jairam.aicodeassistant.platform.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Base type for all domain/application exceptions in AI Software Engineering Assistant.
 *
 * <p>Carries an {@link ErrorType} (machine-readable category), the HTTP status to render, and an
 * optional map of structured properties that are copied into the RFC-9457 {@code ProblemDetail} as
 * extension members. Concrete subclasses (in each bounded context) fix the category/status so
 * controllers never map status codes by hand.
 *
 * <p>This is a {@link RuntimeException} on purpose: application/domain failures are unchecked so
 * they can propagate cleanly to the central {@code GlobalExceptionHandler} without polluting every
 * signature.
 */
public abstract class ApplicationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final ErrorType errorType;
  private final HttpStatus status;
  private final transient Map<String, Object> properties;

  protected ApplicationException(
      ErrorType errorType, HttpStatus status, String message, Map<String, Object> properties) {
    super(message);
    this.errorType = errorType;
    this.status = status;
    this.properties = properties == null ? Map.of() : Map.copyOf(properties);
  }

  protected ApplicationException(
      ErrorType errorType,
      HttpStatus status,
      String message,
      Map<String, Object> properties,
      Throwable cause) {
    super(message, cause);
    this.errorType = errorType;
    this.status = status;
    this.properties = properties == null ? Map.of() : Map.copyOf(properties);
  }

  public ErrorType errorType() {
    return errorType;
  }

  public HttpStatus status() {
    return status;
  }

  /** Additional structured members rendered into the ProblemDetail body. */
  public Map<String, Object> properties() {
    return properties;
  }
}
