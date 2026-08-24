package com.jairam.aicodeassistant.indexing.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** JPA entity for {@code index_jobs}. */
@Entity
@Table(name = "index_jobs")
class IndexJobEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "repository_id", nullable = false, updatable = false)
  private UUID repositoryId;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(name = "clone_url", nullable = false)
  private String cloneUrl;

  @Column(name = "default_branch", nullable = false)
  private String defaultBranch;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "status_detail")
  private String statusDetail;

  @Column(name = "chunk_count")
  private Integer chunkCount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected IndexJobEntity() {
    // Required by JPA.
  }

  IndexJobEntity(
      UUID id,
      UUID repositoryId,
      UUID organizationId,
      String cloneUrl,
      String defaultBranch,
      String status,
      int attempts,
      String statusDetail,
      Integer chunkCount,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.repositoryId = repositoryId;
    this.organizationId = organizationId;
    this.cloneUrl = cloneUrl;
    this.defaultBranch = defaultBranch;
    this.status = status;
    this.attempts = attempts;
    this.statusDetail = statusDetail;
    this.chunkCount = chunkCount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  UUID getId() {
    return id;
  }

  UUID getRepositoryId() {
    return repositoryId;
  }

  UUID getOrganizationId() {
    return organizationId;
  }

  String getCloneUrl() {
    return cloneUrl;
  }

  String getDefaultBranch() {
    return defaultBranch;
  }

  String getStatus() {
    return status;
  }

  int getAttempts() {
    return attempts;
  }

  String getStatusDetail() {
    return statusDetail;
  }

  Integer getChunkCount() {
    return chunkCount;
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
