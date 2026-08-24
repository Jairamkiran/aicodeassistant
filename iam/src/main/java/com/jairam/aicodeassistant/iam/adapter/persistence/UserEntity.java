package com.jairam.aicodeassistant.iam.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code users} table. Kept separate from the {@link
 * com.jairam.aicodeassistant.iam.domain.model.User} aggregate so the domain stays free of JPA
 * annotations (clean hexagonal split); mapping is done by {@link IamPersistenceMapper}.
 *
 * <p>Ids are assigned by the domain (not DB-generated). Because the id is always present, Spring
 * Data's {@code save()} uses {@code merge()} — correct for both insert and update. Optimistic
 * concurrency is guarded by {@link Version}.
 */
@Entity
@Table(name = "users")
class UserEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected UserEntity() {
    // Required by JPA.
  }

  UserEntity(
      UUID id,
      String email,
      String passwordHash,
      String displayName,
      String status,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  UUID getId() {
    return id;
  }

  String getEmail() {
    return email;
  }

  String getPasswordHash() {
    return passwordHash;
  }

  String getDisplayName() {
    return displayName;
  }

  String getStatus() {
    return status;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  long getVersion() {
    return version;
  }
}
