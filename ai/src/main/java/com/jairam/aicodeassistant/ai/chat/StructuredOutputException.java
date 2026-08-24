package com.jairam.aicodeassistant.ai.chat;

/**
 * Thrown when a model reply that was expected to contain structured JSON cannot be located or
 * deserialized. Distinct from {@link ChatException} (a transport/provider failure): here the call
 * succeeded but the payload did not conform.
 */
public class StructuredOutputException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public StructuredOutputException(String message) {
    super(message);
  }

  public StructuredOutputException(String message, Throwable cause) {
    super(message, cause);
  }
}
