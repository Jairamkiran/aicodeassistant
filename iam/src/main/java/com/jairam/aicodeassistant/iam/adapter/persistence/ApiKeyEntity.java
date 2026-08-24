package com.jairam.aicodeassistant.iam.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** JPA entity for the {@code api_keys} table. */
@Entity
@Table(name = "api_keys")
class ApiKeyEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false)
  private String name;

  @Column(name = "key_prefix", nullable = false, updatable = false)
  private String keyPrefix;

  @Column(name = "secret_hash", nullable = false, updatable = false)
  private String secretHash;

  @Column(nullable = false)
  private String scopes;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  protected ApiKeyEntity() {
    // Required by JPA.
  }

  ApiKeyEntity(
      UUID id,
      UUID userId,
      String name,
      String keyPrefix,
      String secretHash,
      String scopes,
      String status,
      Instant createdAt,
      Instant expiresAt,
      Instant lastUsedAt,
      Instant revokedAt) {
    this.id = id;
    this.userId = userId;
    this.name = name;
    this.keyPrefix = keyPrefix;
    this.secretHash = secretHash;
    this.scopes = scopes;
    this.status = status;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
    this.lastUsedAt = lastUsedAt;
    this.revokedAt = revokedAt;
  }

  UUID getId() {
    return id;
  }

  UUID getUserId() {
    return userId;
  }

  String getName() {
    return name;
  }

  String getKeyPrefix() {
    return keyPrefix;
  }

  String getSecretHash() {
    return secretHash;
  }

  String getScopes() {
    return scopes;
  }

  String getStatus() {
    return status;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getExpiresAt() {
    return expiresAt;
  }

  Instant getLastUsedAt() {
    return lastUsedAt;
  }

  Instant getRevokedAt() {
    return revokedAt;
  }
}
