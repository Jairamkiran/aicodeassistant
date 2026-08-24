package com.jairam.aicodeassistant.iam.domain.apikey;

import com.jairam.aicodeassistant.iam.domain.model.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * API key aggregate root — a programmatic credential owned by a user.
 *
 * <p>The presented key has the form {@code aca_<prefix>.<secret>}. The {@code prefix} is a
 * non-secret public identifier used for O(1) lookup; only a <em>hash</em> of the {@code secret} is
 * stored. The aggregate never sees or stores the raw secret beyond the hash — hashing is an adapter
 * concern.
 *
 * <p>{@link #isUsable(Instant)} centralises the "may this key authenticate now?" rule (active, not
 * expired). Scope checks live in {@link #hasScope}.
 */
public final class ApiKey {

  private final ApiKeyId id;
  private final UserId userId;
  private final String name;
  private final String keyPrefix;
  private final String secretHash;
  private final Set<ApiKeyScope> scopes;
  private ApiKeyStatus status;
  private final Instant createdAt;
  private final Instant expiresAt;
  private Instant lastUsedAt;
  private Instant revokedAt;

  private ApiKey(
      ApiKeyId id,
      UserId userId,
      String name,
      String keyPrefix,
      String secretHash,
      Set<ApiKeyScope> scopes,
      ApiKeyStatus status,
      Instant createdAt,
      Instant expiresAt,
      Instant lastUsedAt,
      Instant revokedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.userId = Objects.requireNonNull(userId, "userId");
    this.name = requireText(name, "name");
    this.keyPrefix = requireText(keyPrefix, "keyPrefix");
    this.secretHash = requireText(secretHash, "secretHash");
    this.scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes"));
    this.status = Objects.requireNonNull(status, "status");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.expiresAt = expiresAt; // null = never expires
    this.lastUsedAt = lastUsedAt;
    this.revokedAt = revokedAt;
  }

  /** Issues a new active key. {@code expiresAt} may be null (no expiry). */
  public static ApiKey issue(
      UserId userId,
      String name,
      String keyPrefix,
      String secretHash,
      Set<ApiKeyScope> scopes,
      Instant now,
      Instant expiresAt) {
    return new ApiKey(
        ApiKeyId.newId(),
        userId,
        name,
        keyPrefix,
        secretHash,
        scopes,
        ApiKeyStatus.ACTIVE,
        now,
        expiresAt,
        null,
        null);
  }

  /** Reconstructs a key loaded from persistence. */
  public static ApiKey rehydrate(
      ApiKeyId id,
      UserId userId,
      String name,
      String keyPrefix,
      String secretHash,
      Set<ApiKeyScope> scopes,
      ApiKeyStatus status,
      Instant createdAt,
      Instant expiresAt,
      Instant lastUsedAt,
      Instant revokedAt) {
    return new ApiKey(
        id,
        userId,
        name,
        keyPrefix,
        secretHash,
        scopes,
        status,
        createdAt,
        expiresAt,
        lastUsedAt,
        revokedAt);
  }

  /** True if the key may authenticate right now (active and not expired). */
  public boolean isUsable(Instant now) {
    if (status != ApiKeyStatus.ACTIVE) {
      return false;
    }
    return expiresAt == null || now.isBefore(expiresAt);
  }

  public boolean hasScope(ApiKeyScope scope) {
    return scopes.contains(scope);
  }

  /** Records a successful use for observability. */
  public void markUsed(Instant now) {
    this.lastUsedAt = now;
  }

  /** Revokes the key; idempotent. */
  public void revoke(Instant now) {
    if (status != ApiKeyStatus.REVOKED) {
      this.status = ApiKeyStatus.REVOKED;
      this.revokedAt = now;
    }
  }

  public ApiKeyId id() {
    return id;
  }

  public UserId userId() {
    return userId;
  }

  public String name() {
    return name;
  }

  public String keyPrefix() {
    return keyPrefix;
  }

  public String secretHash() {
    return secretHash;
  }

  public Set<ApiKeyScope> scopes() {
    return scopes;
  }

  public ApiKeyStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  public Instant lastUsedAt() {
    return lastUsedAt;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof ApiKey other && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
