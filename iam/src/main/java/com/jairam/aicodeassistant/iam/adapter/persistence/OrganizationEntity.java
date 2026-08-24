package com.jairam.aicodeassistant.iam.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** JPA entity for the {@code organizations} table. */
@Entity
@Table(name = "organizations")
class OrganizationEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String slug;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected OrganizationEntity() {
    // Required by JPA.
  }

  OrganizationEntity(
      UUID id, String name, String slug, Instant createdAt, Instant updatedAt, long version) {
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  UUID getId() {
    return id;
  }

  String getName() {
    return name;
  }

  String getSlug() {
    return slug;
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
