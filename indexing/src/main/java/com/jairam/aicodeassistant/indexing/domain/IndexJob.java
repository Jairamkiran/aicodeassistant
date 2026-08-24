package com.jairam.aicodeassistant.indexing.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Index-job aggregate — the persistent state of one repository's indexing saga.
 *
 * <p>State transitions are driven by {@link
 * com.jairam.aicodeassistant.indexing.application.IndexingSaga}. The aggregate enforces only local
 * invariants (advancing status, recording failure/attempts); the atomic "claim" (exactly-once
 * acquisition) is a persistence concern implemented as a conditional update in the store.
 */
public final class IndexJob {

  private final UUID id;
  private final UUID repositoryId;
  private final UUID organizationId;
  private final String cloneUrl;
  private final String defaultBranch;
  private IndexStatus status;
  private int attempts;
  private String statusDetail;
  private Integer chunkCount;
  private final Instant createdAt;
  private Instant updatedAt;
  private final long version;

  private IndexJob(
      UUID id,
      UUID repositoryId,
      UUID organizationId,
      String cloneUrl,
      String defaultBranch,
      IndexStatus status,
      int attempts,
      String statusDetail,
      Integer chunkCount,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.repositoryId = Objects.requireNonNull(repositoryId, "repositoryId");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.cloneUrl = requireText(cloneUrl, "cloneUrl");
    this.defaultBranch = requireText(defaultBranch, "defaultBranch");
    this.status = Objects.requireNonNull(status, "status");
    this.attempts = attempts;
    this.statusDetail = statusDetail;
    this.chunkCount = chunkCount;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.version = version;
  }

  /** Creates a fresh job in REGISTERED state (on receiving an import request). */
  public static IndexJob register(
      UUID repositoryId, UUID organizationId, String cloneUrl, String defaultBranch, Instant now) {
    return new IndexJob(
        UUID.randomUUID(),
        repositoryId,
        organizationId,
        cloneUrl,
        defaultBranch,
        IndexStatus.REGISTERED,
        0,
        null,
        null,
        now,
        now,
        0L);
  }

  /** Reconstructs from persistence. */
  public static IndexJob rehydrate(
      UUID id,
      UUID repositoryId,
      UUID organizationId,
      String cloneUrl,
      String defaultBranch,
      IndexStatus status,
      int attempts,
      String statusDetail,
      Integer chunkCount,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    return new IndexJob(
        id,
        repositoryId,
        organizationId,
        cloneUrl,
        defaultBranch,
        status,
        attempts,
        statusDetail,
        chunkCount,
        createdAt,
        updatedAt,
        version);
  }

  /** Advances to the next saga step. */
  public void transitionTo(IndexStatus next, Instant now) {
    this.status = Objects.requireNonNull(next, "next");
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  /** Marks the job successfully indexed with the chunk count. */
  public void markIndexed(int chunks, Instant now) {
    this.status = IndexStatus.INDEXED;
    this.chunkCount = chunks;
    this.statusDetail = null;
    this.updatedAt = now;
  }

  /** Marks the job failed with a reason; increments the attempt counter. */
  public void markFailed(String reason, Instant now) {
    this.status = IndexStatus.FAILED;
    this.statusDetail = reason;
    this.attempts += 1;
    this.updatedAt = now;
  }

  public UUID id() {
    return id;
  }

  public UUID repositoryId() {
    return repositoryId;
  }

  public UUID organizationId() {
    return organizationId;
  }

  public String cloneUrl() {
    return cloneUrl;
  }

  public String defaultBranch() {
    return defaultBranch;
  }

  public IndexStatus status() {
    return status;
  }

  public int attempts() {
    return attempts;
  }

  public String statusDetail() {
    return statusDetail;
  }

  public Integer chunkCount() {
    return chunkCount;
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
}
