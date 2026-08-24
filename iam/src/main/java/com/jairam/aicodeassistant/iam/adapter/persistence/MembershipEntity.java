package com.jairam.aicodeassistant.iam.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** JPA entity for the {@code memberships} table. */
@Entity
@Table(name = "memberships")
class MembershipEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(nullable = false)
  private String role;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected MembershipEntity() {
    // Required by JPA.
  }

  MembershipEntity(
      UUID id, UUID userId, UUID organizationId, String role, Instant createdAt, long version) {
    this.id = id;
    this.userId = userId;
    this.organizationId = organizationId;
    this.role = role;
    this.createdAt = createdAt;
    this.version = version;
  }

  UUID getId() {
    return id;
  }

  UUID getUserId() {
    return userId;
  }

  UUID getOrganizationId() {
    return organizationId;
  }

  String getRole() {
    return role;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  long getVersion() {
    return version;
  }
}
