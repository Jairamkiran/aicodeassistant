package com.jairam.aicodeassistant.platform.web;

import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import com.jairam.aicodeassistant.platform.observability.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Central translation of exceptions into RFC-9457 {@link ProblemDetail} responses. Every error body
 * is uniform: a stable {@code type} URN, a {@code code}, a {@code correlationId} (for
 * cross-referencing logs/traces), and a server {@code timestamp}. Field-level validation failures
 * are expanded into a structured {@code errors} array.
 *
 * <p>Logging policy: 4xx client errors are logged at WARN without stack traces (they are expected),
 * 5xx server errors at ERROR with the stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final Clock clock;

  public GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  /** Application/domain exceptions with an explicit category + status. */
  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ProblemDetail> handleApplication(
      ApplicationException ex, HttpServletRequest request) {
    ProblemDetail problem = base(ex.status(), ex.errorType(), ex.getMessage(), request);
    ex.properties().forEach(problem::setProperty);
    logByStatus(ex.status(), ex, request);
    return ResponseEntity.status(ex.status()).body(problem);
  }

  /** Bean-validation failures on {@code @Valid} @RequestBody arguments. */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problem =
        base(
            HttpStatus.BAD_REQUEST,
            ErrorType.VALIDATION,
            ErrorType.VALIDATION.defaultTitle(),
            servletRequest(request));
    List<Map<String, Object>> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::describeFieldError)
            .collect(Collectors.toList());
    problem.setProperty("errors", fieldErrors);
    log.warn("Validation failed: {} field error(s)", fieldErrors.size());
    return ResponseEntity.badRequest().body(problem);
  }

  /** Bean-validation failures on {@code @RequestParam}/{@code @PathVariable}. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    ProblemDetail problem =
        base(
            HttpStatus.BAD_REQUEST,
            ErrorType.VALIDATION,
            ErrorType.VALIDATION.defaultTitle(),
            request);
    List<Map<String, Object>> violations =
        ex.getConstraintViolations().stream()
            .map(GlobalExceptionHandler::describeViolation)
            .collect(Collectors.toList());
    problem.setProperty("errors", violations);
    log.warn("Constraint violation: {} error(s)", violations.size());
    return ResponseEntity.badRequest().body(problem);
  }

  /** Fallback: anything uncaught becomes a 500 without leaking internals. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
    ProblemDetail problem =
        base(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorType.INTERNAL,
            "An unexpected error occurred. Reference the correlationId when reporting this.",
            request);
    log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
    return ResponseEntity.internalServerError().body(problem);
  }

  // --- helpers ---------------------------------------------------------------

  private ProblemDetail base(
      HttpStatus status, ErrorType type, String detail, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create(type.type()));
    problem.setTitle(type.defaultTitle());
    problem.setProperty("code", type.code());
    problem.setProperty("correlationId", CorrelationId.current());
    problem.setProperty("timestamp", OffsetDateTime.now(clock).toString());
    if (request != null) {
      problem.setInstance(URI.create(request.getRequestURI()));
    }
    return problem;
  }

  private void logByStatus(HttpStatus status, Exception ex, HttpServletRequest request) {
    if (status.is5xxServerError()) {
      log.error("{} on {} {}", status, request.getMethod(), request.getRequestURI(), ex);
    } else {
      log.warn(
          "{} on {} {}: {}", status, request.getMethod(), request.getRequestURI(), ex.getMessage());
    }
  }

  private static Map<String, Object> describeFieldError(FieldError fe) {
    return Map.of(
        "field", fe.getField(),
        "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
        "rejectedValue", String.valueOf(fe.getRejectedValue()));
  }

  private static Map<String, Object> describeViolation(ConstraintViolation<?> v) {
    return Map.of("field", v.getPropertyPath().toString(), "message", v.getMessage());
  }

  private static HttpServletRequest servletRequest(WebRequest request) {
    if (request instanceof org.springframework.web.context.request.ServletWebRequest swr) {
      return swr.getRequest();
    }
    return null;
  }
}
