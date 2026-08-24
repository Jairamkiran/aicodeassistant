package com.jairam.aicodeassistant.platform.crypto;

/**
 * Symmetric encryption for secrets that must be stored recoverably (unlike passwords, which are
 * hashed). The prime use case is third-party provider tokens (e.g. a GitHub OAuth access token)
 * that the system must later present back to the provider — so they cannot be one-way hashed.
 *
 * <p>Implementations must provide authenticated encryption (AES-GCM) so tampering with ciphertext
 * is detected on decrypt. The returned ciphertext is a self-contained, storable string
 * (algorithm/version + IV + ciphertext).
 */
public interface EncryptionService {

  /** Encrypts UTF-8 plaintext, returning an opaque, storable ciphertext string. */
  String encrypt(String plaintext);

  /**
   * Decrypts a ciphertext produced by {@link #encrypt}.
   *
   * @throws com.jairam.aicodeassistant.platform.crypto.DecryptionException if the ciphertext is
   *     malformed or fails the authentication tag (tampering/wrong key)
   */
  String decrypt(String ciphertext);
}
