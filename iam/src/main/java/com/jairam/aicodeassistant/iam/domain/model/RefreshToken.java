package com.jairam.aicodeassistant.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A single refresh token within a rotation <em>family</em>.
 *
 * <p>A login starts a family (a fresh {@code familyId}). Each rotation issues a new token in the
 * same family and marks the previous one {@link #markUsed}. A token is {@link #isActive} only while
 * it is neither used, revoked, nor expired.
 *
 * <p>Only the token <em>hash</em> is stored — never the raw secret — so a database leak does not
 * yield usable tokens. Reuse detection (revoking the whole family when an already-used/revoked
 * token is presented) is orchestrated by the application layer using {@link #isActive}.
 */
public final class RefreshToken {

  private final UUID id;
  private final UserId userId;
  private final UUID familyId;
  private final String tokenHash;
  private final Instant issuedAt;
  private final Instant expiresAt;
  private Instant usedAt;
  private Instant revokedAt;

  private RefreshToken(
      UUID id,
      UserId userId,
      UUID familyId,
      String tokenHash,
      Instant issuedAt,
      Instant expiresAt,
      Instant usedAt,
      Instant revokedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.userId = Objects.requireNonNull(userId, "userId");
    this.familyId = Objects.requireNonNull(familyId, "familyId");
    this.tokenHash = requireText(tokenHash, "tokenHash");
    this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    this.usedAt = usedAt;
    this.revokedAt = revokedAt;
  }

  /** Issues the first token of a brand-new family (called at login). */
  public static RefreshToken issueNewFamily(
      UserId userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
    return new RefreshToken(
        UUID.randomUUID(), userId, UUID.randomUUID(), tokenHash, issuedAt, expiresAt, null, null);
  }

  /** Issues the next token in an existing family (called on rotation). */
  public static RefreshToken issueInFamily(
      UserId userId, UUID familyId, String tokenHash, Instant issuedAt, Instant expiresAt) {
    return new RefreshToken(
        UUID.randomUUID(), userId, familyId, tokenHash, issuedAt, expiresAt, null, null);
  }

  /** Reconstructs a token loaded from persistence. */
  public static RefreshToken rehydrate(
      UUID id,
      UserId userId,
      UUID familyId,
      String tokenHash,
      Instant issuedAt,
      Instant expiresAt,
      Instant usedAt,
      Instant revokedAt) {
    return new RefreshToken(
        id, userId, familyId, tokenHash, issuedAt, expiresAt, usedAt, revokedAt);
  }

  /** Usable for exactly one rotation: not used, not revoked, not expired. */
  public boolean isActive(Instant now) {
    return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
  }

  /** True once this token has been consumed by a rotation. */
  public boolean isUsed() {
    return usedAt != null;
  }

  /** True if this token was explicitly revoked. */
  public boolean isRevoked() {
    return revokedAt != null;
  }

  /** Marks this token consumed by a rotation. Idempotent-safe: first call wins. */
  public void markUsed(Instant now) {
    if (usedAt == null) {
      this.usedAt = Objects.requireNonNull(now, "now");
    }
  }

  /** Marks this token revoked (e.g. logout or family compromise). */
  public void revoke(Instant now) {
    if (revokedAt == null) {
      this.revokedAt = Objects.requireNonNull(now, "now");
    }
  }

  public UUID id() {
    return id;
  }

  public UserId userId() {
    return userId;
  }

  public UUID familyId() {
    return familyId;
  }

  public String tokenHash() {
    return tokenHash;
  }

  public Instant issuedAt() {
    return issuedAt;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  public Instant usedAt() {
    return usedAt;
  }

  public Instant revokedAt() {
    return revokedAt;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
