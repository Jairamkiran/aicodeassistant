package com.jairam.aicodeassistant.iam.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** JPA entity for the {@code refresh_tokens} table. */
@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "family_id", nullable = false)
  private UUID familyId;

  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;

  @Column(name = "issued_at", nullable = false, updatable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  protected RefreshTokenEntity() {
    // Required by JPA.
  }

  RefreshTokenEntity(
      UUID id,
      UUID userId,
      UUID familyId,
      String tokenHash,
      Instant issuedAt,
      Instant expiresAt,
      Instant usedAt,
      Instant revokedAt) {
    this.id = id;
    this.userId = userId;
    this.familyId = familyId;
    this.tokenHash = tokenHash;
    this.issuedAt = issuedAt;
    this.expiresAt = expiresAt;
    this.usedAt = usedAt;
    this.revokedAt = revokedAt;
  }

  UUID getId() {
    return id;
  }

  UUID getUserId() {
    return userId;
  }

  UUID getFamilyId() {
    return familyId;
  }

  String getTokenHash() {
    return tokenHash;
  }

  Instant getIssuedAt() {
    return issuedAt;
  }

  Instant getExpiresAt() {
    return expiresAt;
  }

  Instant getUsedAt() {
    return usedAt;
  }

  Instant getRevokedAt() {
    return revokedAt;
  }
}
