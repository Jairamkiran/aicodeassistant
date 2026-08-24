package com.jairam.aicodeassistant.platform.crypto;

/**
 * Thrown when decryption fails — malformed ciphertext, a wrong key, or a failed authentication tag
 * (tampering). Deliberately does not include the ciphertext or key material in its message.
 */
public class DecryptionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DecryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
