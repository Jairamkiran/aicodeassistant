package com.jairam.aicodeassistant.repository.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** JPA entity for the {@code repositories} table. */
@Entity
@Table(name = "repositories")
class RepositoryEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(nullable = false, updatable = false)
  private String provider;

  @Column(name = "external_id", nullable = false, updatable = false)
  private String externalId;

  @Column(nullable = false)
  private String owner;

  @Column(nullable = false)
  private String name;

  @Column(name = "clone_url", nullable = false)
  private String cloneUrl;

  @Column(name = "default_branch", nullable = false)
  private String defaultBranch;

  @Column(name = "is_private", nullable = false)
  private boolean isPrivate;

  @Column(nullable = false)
  private String status;

  @Column(name = "registered_by", nullable = false, updatable = false)
  private UUID registeredBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "status_detail")
  private String statusDetail;

  @Version
  @Column(nullable = false)
  private long version;

  protected RepositoryEntity() {
    // Required by JPA.
  }

  RepositoryEntity(
      UUID id,
      UUID organizationId,
      String provider,
      String externalId,
      String owner,
      String name,
      String cloneUrl,
      String defaultBranch,
      boolean isPrivate,
      String status,
      UUID registeredBy,
      Instant createdAt,
      Instant updatedAt,
      String statusDetail,
      long version) {
    this.id = id;
    this.organizationId = organizationId;
    this.provider = provider;
    this.externalId = externalId;
    this.owner = owner;
    this.name = name;
    this.cloneUrl = cloneUrl;
    this.defaultBranch = defaultBranch;
    this.isPrivate = isPrivate;
    this.status = status;
    this.registeredBy = registeredBy;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.statusDetail = statusDetail;
    this.version = version;
  }

  UUID getId() {
    return id;
  }

  UUID getOrganizationId() {
    return organizationId;
  }

  String getProvider() {
    return provider;
  }

  String getExternalId() {
    return externalId;
  }

  String getOwner() {
    return owner;
  }

  String getName() {
    return name;
  }

  String getCloneUrl() {
    return cloneUrl;
  }

  String getDefaultBranch() {
    return defaultBranch;
  }

  boolean isPrivate() {
    return isPrivate;
  }

  String getStatus() {
    return status;
  }

  UUID getRegisteredBy() {
    return registeredBy;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  String getStatusDetail() {
    return statusDetail;
  }

  long getVersion() {
    return version;
  }
}
