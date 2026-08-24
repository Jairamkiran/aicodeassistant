package com.jairam.aicodeassistant.ai.chat;

import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Base type for chat-provider failures, surfaced as domain errors so provider HTTP/JSON specifics
 * never leak. {@link Unavailable} is transient/retryable; {@link CredentialRejected} is a
 * configuration error (do not retry).
 */
public class ChatException extends ApplicationException {

  private static final long serialVersionUID = 1L;

  protected ChatException(
      ErrorType type, HttpStatus status, String message, Map<String, Object> properties) {
    super(type, status, message, properties);
  }

  /** Provider unreachable / timeout / 429 / 5xx — retryable. HTTP 503. */
  public static final class Unavailable extends ChatException {
    private static final long serialVersionUID = 1L;

    public Unavailable(String provider, String detail) {
      super(
          ErrorType.DEPENDENCY_UNAVAILABLE,
          HttpStatus.SERVICE_UNAVAILABLE,
          "Chat provider unavailable: " + detail,
          Map.of("provider", provider));
    }
  }

  /** Provider rejected the credential (401/403) — not retryable. HTTP 502. */
  public static final class CredentialRejected extends ChatException {
    private static final long serialVersionUID = 1L;

    public CredentialRejected(String provider) {
      super(
          ErrorType.DEPENDENCY_UNAVAILABLE,
          HttpStatus.BAD_GATEWAY,
          "Chat provider rejected the configured credential",
          Map.of("provider", provider));
    }
  }
}
