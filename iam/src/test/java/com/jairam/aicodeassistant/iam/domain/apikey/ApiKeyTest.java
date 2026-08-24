package com.jairam.aicodeassistant.iam.domain.apikey;

import static org.assertj.core.api.Assertions.assertThat;

import com.jairam.aicodeassistant.iam.domain.model.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiKeyTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
  private static final UserId USER = UserId.newId();

  private ApiKey active(Instant expiresAt) {
    return ApiKey.issue(
        USER, "ci", "aca_pfx", "hash", ApiKeyScope.setOf(ApiKeyScope.READ), NOW, expiresAt);
  }

  @Test
  void newKeyWithoutExpiryIsUsable() {
    assertThat(active(null).isUsable(NOW.plusSeconds(999999))).isTrue();
  }

  @Test
  void keyIsUsableBeforeExpiryAndNotAfter() {
    ApiKey key = active(NOW.plusSeconds(3600));
    assertThat(key.isUsable(NOW.plusSeconds(60))).isTrue();
    assertThat(key.isUsable(NOW.plusSeconds(3601))).isFalse();
  }

  @Test
  void revokedKeyIsNotUsable() {
    ApiKey key = active(null);
    key.revoke(NOW.plusSeconds(10));
    assertThat(key.isUsable(NOW.plusSeconds(20))).isFalse();
    assertThat(key.status()).isEqualTo(ApiKeyStatus.REVOKED);
  }

  @Test
  void revokeIsIdempotentKeepingFirstTimestamp() {
    ApiKey key = active(null);
    key.revoke(NOW.plusSeconds(10));
    key.revoke(NOW.plusSeconds(20));
    assertThat(key.revokedAt()).isEqualTo(NOW.plusSeconds(10));
  }

  @Test
  void scopeCheckReflectsGrantedScopes() {
    ApiKey key =
        ApiKey.issue(
            USER,
            "ci",
            "aca_pfx",
            "hash",
            ApiKeyScope.setOf(ApiKeyScope.READ, ApiKeyScope.WRITE),
            NOW,
            null);
    assertThat(key.hasScope(ApiKeyScope.READ)).isTrue();
    assertThat(key.hasScope(ApiKeyScope.WRITE)).isTrue();
    assertThat(key.hasScope(ApiKeyScope.ADMIN)).isFalse();
  }

  @Test
  void scopeCsvRoundTrips() {
    var scopes = ApiKeyScope.setOf(ApiKeyScope.READ, ApiKeyScope.ADMIN);
    String csv = ApiKeyScope.toCsv(scopes);
    assertThat(ApiKeyScope.parse(csv))
        .containsExactlyInAnyOrder(ApiKeyScope.READ, ApiKeyScope.ADMIN);
  }

  @Test
  void scopeParseIgnoresBlanksAndUnknowns() {
    assertThat(ApiKeyScope.parse("READ, , BOGUS,write"))
        .containsExactlyInAnyOrder(ApiKeyScope.READ, ApiKeyScope.WRITE);
    assertThat(ApiKeyScope.parse("")).isEmpty();
  }
}
