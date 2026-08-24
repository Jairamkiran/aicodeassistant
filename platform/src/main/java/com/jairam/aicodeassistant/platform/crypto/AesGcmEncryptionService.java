package com.jairam.aicodeassistant.platform.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM implementation of {@link EncryptionService}.
 *
 * <p>Each {@link #encrypt} generates a fresh random 96-bit IV (GCM's recommended nonce size) and
 * produces a self-describing string: {@code v1:<base64url(iv)>:<base64url(ciphertext||tag)>}. The
 * {@code v1} prefix lets the scheme evolve (e.g. key rotation, algorithm change) without breaking
 * existing ciphertext. GCM's 128-bit authentication tag means any tampering — or a wrong key —
 * fails on decrypt with a {@link DecryptionException} rather than silently returning garbage.
 */
public class AesGcmEncryptionService implements EncryptionService {

  private static final String VERSION = "v1";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int IV_BYTES = 12; // 96-bit nonce (GCM recommended)
  private static final int TAG_BITS = 128;

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
  private final Base64.Decoder decoder = Base64.getUrlDecoder();

  /**
   * @param base64Key Base64-encoded key; must decode to exactly 32 bytes (AES-256)
   */
  public AesGcmEncryptionService(String base64Key) {
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(base64Key);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("aicodeassistant.crypto.key is not valid Base64", e);
    }
    if (keyBytes.length != 32) {
      throw new IllegalStateException(
          "aicodeassistant.crypto.key must decode to 32 bytes (AES-256); got " + keyBytes.length);
    }
    this.key = new SecretKeySpec(keyBytes, "AES");
  }

  @Override
  public String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return VERSION + ":" + encoder.encodeToString(iv) + ":" + encoder.encodeToString(ciphertext);
    } catch (java.security.GeneralSecurityException e) {
      // Misconfiguration (bad key/algorithm) — not recoverable at runtime.
      throw new IllegalStateException("Encryption failed", e);
    }
  }

  @Override
  public String decrypt(String ciphertext) {
    String[] parts = ciphertext == null ? new String[0] : ciphertext.split(":");
    if (parts.length != 3 || !VERSION.equals(parts[0])) {
      throw new DecryptionException("Malformed or unsupported ciphertext format", null);
    }
    try {
      byte[] iv = decoder.decode(parts[1]);
      byte[] payload = decoder.decode(parts[2]);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
    } catch (java.security.GeneralSecurityException | IllegalArgumentException e) {
      // Wrong key, tampered ciphertext (tag mismatch), or corrupt encoding.
      throw new DecryptionException("Decryption failed", e);
    }
  }
}
