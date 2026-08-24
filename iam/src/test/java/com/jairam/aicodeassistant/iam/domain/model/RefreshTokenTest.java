package com.jairam.aicodeassistant.iam.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
  private static final UserId USER = UserId.newId();

  @Test
  void newTokenIsActiveBeforeExpiry() {
    RefreshToken token = RefreshToken.issueNewFamily(USER, "hash", NOW, NOW.plusSeconds(3600));

    assertThat(token.isActive(NOW)).isTrue();
    assertThat(token.isUsed()).isFalse();
    assertThat(token.isRevoked()).isFalse();
  }

  @Test
  void expiredTokenIsNotActive() {
    RefreshToken token = RefreshToken.issueNewFamily(USER, "hash", NOW, NOW.plusSeconds(60));
    assertThat(token.isActive(NOW.plusSeconds(120))).isFalse();
  }

  @Test
  void usedTokenIsNotActive() {
    RefreshToken token = RefreshToken.issueNewFamily(USER, "hash", NOW, NOW.plusSeconds(3600));
    token.markUsed(NOW.plusSeconds(10));

    assertThat(token.isUsed()).isTrue();
    assertThat(token.isActive(NOW.plusSeconds(20))).isFalse();
  }

  @Test
  void revokedTokenIsNotActive() {
    RefreshToken token = RefreshToken.issueNewFamily(USER, "hash", NOW, NOW.plusSeconds(3600));
    token.revoke(NOW.plusSeconds(10));

    assertThat(token.isRevoked()).isTrue();
    assertThat(token.isActive(NOW.plusSeconds(20))).isFalse();
  }

  @Test
  void markUsedIsIdempotentFirstWins() {
    RefreshToken token = RefreshToken.issueNewFamily(USER, "hash", NOW, NOW.plusSeconds(3600));
    token.markUsed(NOW.plusSeconds(10));
    token.markUsed(NOW.plusSeconds(20));

    assertThat(token.usedAt()).isEqualTo(NOW.plusSeconds(10));
  }

  @Test
  void tokenIssuedInFamilySharesFamilyId() {
    RefreshToken first = RefreshToken.issueNewFamily(USER, "h1", NOW, NOW.plusSeconds(3600));
    RefreshToken second =
        RefreshToken.issueInFamily(USER, first.familyId(), "h2", NOW, NOW.plusSeconds(3600));

    assertThat(second.familyId()).isEqualTo(first.familyId());
    assertThat(second.id()).isNotEqualTo(first.id());
  }
}
