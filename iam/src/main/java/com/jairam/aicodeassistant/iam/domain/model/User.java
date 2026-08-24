package com.jairam.aicodeassistant.iam.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * User aggregate root.
 *
 * <p>Holds identity, credentials (only ever the <em>hashed</em> password — the domain never stores
 * plaintext), display name, lifecycle status, and optimistic concurrency version. Construction goes
 * through {@link #register} (new user) or {@link #rehydrate} (loaded from persistence) so
 * invariants hold for every instance.
 *
 * <p>This is pure domain code: no Spring, JPA, or servlet imports. Password hashing/verification is
 * delegated to an application-layer port, keeping the aggregate free of cryptography concerns.
 */
public final class User {

  private final UserId id;
  private final Email email;
  private String passwordHash;
  private String displayName;
  private UserStatus status;
  private final Instant createdAt;
  private Instant updatedAt;
  private long version;

  private User(
      UserId id,
      Email email,
      String passwordHash,
      String displayName,
      UserStatus status,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.email = Objects.requireNonNull(email, "email");
    this.passwordHash = requireText(passwordHash, "passwordHash");
    this.displayName = requireText(displayName, "displayName");
    this.status = Objects.requireNonNull(status, "status");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.version = version;
  }

  /**
   * Registers a brand-new active user.
   *
   * @param email the (already validated) email value object
   * @param passwordHash the pre-computed password hash (never plaintext)
   * @param displayName human-facing name
   * @param now current instant from the application clock
   */
  public static User register(Email email, String passwordHash, String displayName, Instant now) {
    return new User(
        UserId.newId(), email, passwordHash, displayName, UserStatus.ACTIVE, now, now, 0L);
  }

  /** Reconstructs a user loaded from persistence without re-running creation logic. */
  public static User rehydrate(
      UserId id,
      Email email,
      String passwordHash,
      String displayName,
      UserStatus status,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    return new User(id, email, passwordHash, displayName, status, createdAt, updatedAt, version);
  }

  /** True only if the account may currently authenticate. */
  public boolean canAuthenticate() {
    return status == UserStatus.ACTIVE;
  }

  /** Replaces the stored password hash (e.g. after a password change). */
  public void changePasswordHash(String newHash, Instant now) {
    this.passwordHash = requireText(newHash, "newHash");
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  /** Updates the display name, enforcing non-blank. */
  public void rename(String newDisplayName, Instant now) {
    this.displayName = requireText(newDisplayName, "newDisplayName");
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  /** Disables the account so it can no longer authenticate. */
  public void disable(Instant now) {
    this.status = UserStatus.DISABLED;
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  public UserId id() {
    return id;
  }

  public Email email() {
    return email;
  }

  public String passwordHash() {
    return passwordHash;
  }

  public String displayName() {
    return displayName;
  }

  public UserStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public long version() {
    return version;
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
    return o instanceof User other && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
