package com.jairam.aicodeassistant.platform.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesGcmEncryptionServiceTest {

  // A fixed 32-byte (256-bit) test key, Base64-encoded.
  private static final String KEY =
      Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

  private final EncryptionService service = new AesGcmEncryptionService(KEY);

  @Test
  void encryptThenDecryptRoundTrips() {
    String plaintext = "gho_a-github-oauth-token-value";
    String ciphertext = service.encrypt(plaintext);

    assertThat(ciphertext).startsWith("v1:").isNotEqualTo(plaintext);
    assertThat(service.decrypt(ciphertext)).isEqualTo(plaintext);
  }

  @Test
  void eachEncryptionUsesAFreshIvSoCiphertextDiffers() {
    String plaintext = "same-input";
    assertThat(service.encrypt(plaintext)).isNotEqualTo(service.encrypt(plaintext));
  }

  @Test
  void tamperedCiphertextIsRejected() {
    String ciphertext = service.encrypt("secret");
    // Flip a character in the ciphertext payload → GCM tag check must fail.
    String tampered = ciphertext.substring(0, ciphertext.length() - 2) + "AA";
    assertThatThrownBy(() -> service.decrypt(tampered)).isInstanceOf(DecryptionException.class);
  }

  @Test
  void wrongKeyCannotDecrypt() {
    String ciphertext = service.encrypt("secret");
    var otherService =
        new AesGcmEncryptionService(
            Base64.getEncoder().encodeToString("ffffffffffffffffffffffffffffffff".getBytes()));
    assertThatThrownBy(() -> otherService.decrypt(ciphertext))
        .isInstanceOf(DecryptionException.class);
  }

  @Test
  void malformedCiphertextIsRejected() {
    assertThatThrownBy(() -> service.decrypt("not-valid")).isInstanceOf(DecryptionException.class);
    assertThatThrownBy(() -> service.decrypt(null)).isInstanceOf(DecryptionException.class);
  }

  @Test
  void keyMustBe256Bit() {
    assertThatThrownBy(
            () ->
                new AesGcmEncryptionService(Base64.getEncoder().encodeToString("short".getBytes())))
        .isInstanceOf(IllegalStateException.class);
  }
}
