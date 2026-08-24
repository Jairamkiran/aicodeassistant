package com.jairam.aicodeassistant.iam.adapter.security;

import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Generates and verifies API keys of the form {@code aca_<prefix>.<secret>}.
 *
 * <ul>
 *   <li><b>prefix</b>: 9 URL-safe chars of randomness — a non-secret public identifier for O(1)
 *       lookup and for humans to recognise a key.
 *   <li><b>secret</b>: 256 bits of {@link SecureRandom} entropy, URL-safe Base64. Only its SHA-256
 *       hash is stored; verification is deterministic hash comparison. (BCrypt is unnecessary for a
 *       high-entropy random secret and would preclude hash-based lookup.)
 * </ul>
 */
@Component
class SecureRandomApiKeyGenerator implements ApiKeyGenerator {

  private static final String PUBLIC_PREFIX = "aca_";
  private static final int PREFIX_BYTES = 6; // ~8 Base64 chars
  private static final int SECRET_BYTES = 32; // 256 bits

  private final SecureRandom random = new SecureRandom();

  @Override
  public GeneratedApiKey generate() {
    String prefix = PUBLIC_PREFIX + randomToken(PREFIX_BYTES);
    String secret = randomToken(SECRET_BYTES);
    String rawKey = prefix + "." + secret;
    return new GeneratedApiKey(rawKey, prefix, sha256(secret));
  }

  @Override
  public Optional<ParsedApiKey> parse(String rawKey) {
    if (rawKey == null) {
      return Optional.empty();
    }
    String trimmed = rawKey.trim();
    int dot = trimmed.indexOf('.');
    if (!trimmed.startsWith(PUBLIC_PREFIX) || dot <= 0 || dot == trimmed.length() - 1) {
      return Optional.empty();
    }
    String prefix = trimmed.substring(0, dot);
    String secret = trimmed.substring(dot + 1);
    return Optional.of(new ParsedApiKey(prefix, secret));
  }

  @Override
  public boolean matches(String presentedSecret, String storedHash) {
    String presentedHash = sha256(presentedSecret);
    // Constant-time comparison to avoid a timing side-channel on the hash.
    return MessageDigest.isEqual(
        presentedHash.getBytes(StandardCharsets.UTF_8),
        storedHash.getBytes(StandardCharsets.UTF_8));
  }

  private String randomToken(int bytes) {
    byte[] buf = new byte[bytes];
    random.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
