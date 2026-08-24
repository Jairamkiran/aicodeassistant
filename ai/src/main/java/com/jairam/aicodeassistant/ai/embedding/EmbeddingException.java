package com.jairam.aicodeassistant.ai.embedding;

import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Raised when the embedding provider is unavailable (timeout, 5xx, connection error) after retries,
 * or returns an unusable response. Surfaced as a domain error (HTTP 503) — provider HTTP/JSON
 * specifics never leak.
 */
public class EmbeddingException extends ApplicationException {

  private static final long serialVersionUID = 1L;

  public EmbeddingException(String detail) {
    super(
        ErrorType.DEPENDENCY_UNAVAILABLE,
        HttpStatus.SERVICE_UNAVAILABLE,
        "Embedding provider unavailable: " + detail,
        Map.of("provider", "ollama"));
  }
}
