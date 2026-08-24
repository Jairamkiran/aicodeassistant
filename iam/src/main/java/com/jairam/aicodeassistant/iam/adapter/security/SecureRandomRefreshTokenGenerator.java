package com.jairam.aicodeassistant.iam.adapter.security;

import com.jairam.aicodeassistant.iam.domain.port.RefreshTokenGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Generates opaque refresh tokens and their storage hashes.
 *
 * <p>The raw secret is 256 bits of {@link SecureRandom} entropy, URL-safe Base64-encoded, and
 * returned to the client once. Only the SHA-256 hash of the secret is persisted, so a database
 * compromise does not expose usable tokens. SHA-256 (not BCrypt) is appropriate here because the
 * secret is already high-entropy random — a slow KDF adds cost without security benefit, and we
 * need deterministic hashing for lookup by hash.
 */
@Component
class SecureRandomRefreshTokenGenerator implements RefreshTokenGenerator {

  private static final int TOKEN_BYTES = 32; // 256 bits.
  private final SecureRandom random = new SecureRandom();

  @Override
  public GeneratedRefreshToken generate() {
    byte[] raw = new byte[TOKEN_BYTES];
    random.nextBytes(raw);
    String rawValue = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    return new GeneratedRefreshToken(rawValue, hash(rawValue));
  }

  @Override
  public String hash(String rawValue) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (java.security.NoSuchAlgorithmException e) {
      // SHA-256 is guaranteed present on every JVM; this cannot happen.
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
