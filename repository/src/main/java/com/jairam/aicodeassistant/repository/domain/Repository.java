package com.jairam.aicodeassistant.repository.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Repository aggregate root — an external repository registered into an organization for indexing.
 *
 * <p>M3 covers registration and the import request; the state machine beyond {@code REGISTERED} is
 * driven by the indexing worker (M4). {@code provider} is fixed to {@code "GITHUB"} in M3 (a second
 * provider would justify an enum/VO — not built speculatively).
 */
public final class Repository {

  private final RepositoryId id;
  private final UUID organizationId;
  private final String provider;
  private final String externalId;
  private final String owner;
  private final String name;
  private final String cloneUrl;
  private final String defaultBranch;
  private final boolean isPrivate;
  private ImportStatus status;
  private final UUID registeredBy;
  private final Instant createdAt;
  private Instant updatedAt;
  private String statusDetail;
  private final long version;

  private Repository(
      RepositoryId id,
      UUID organizationId,
      String provider,
      String externalId,
      String owner,
      String name,
      String cloneUrl,
      String defaultBranch,
      boolean isPrivate,
      ImportStatus status,
      UUID registeredBy,
      Instant createdAt,
      Instant updatedAt,
      String statusDetail,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.provider = requireText(provider, "provider");
    this.externalId = requireText(externalId, "externalId");
    this.owner = requireText(owner, "owner");
    this.name = requireText(name, "name");
    this.cloneUrl = requireText(cloneUrl, "cloneUrl");
    this.defaultBranch = requireText(defaultBranch, "defaultBranch");
    this.isPrivate = isPrivate;
    this.status = Objects.requireNonNull(status, "status");
    this.registeredBy = Objects.requireNonNull(registeredBy, "registeredBy");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.statusDetail = statusDetail;
    this.version = version;
  }

  /** Registers a new repository in {@code REGISTERED} state. */
  public static Repository register(
      UUID organizationId,
      String provider,
      String externalId,
      String owner,
      String name,
      String cloneUrl,
      String defaultBranch,
      boolean isPrivate,
      UUID registeredBy,
      Instant now) {
    return new Repository(
        RepositoryId.newId(),
        organizationId,
        provider,
        externalId,
        owner,
        name,
        cloneUrl,
        defaultBranch,
        isPrivate,
        ImportStatus.REGISTERED,
        registeredBy,
        now,
        now,
        null,
        0L);
  }

  /** Reconstructs from persistence. */
  public static Repository rehydrate(
      RepositoryId id,
      UUID organizationId,
      String provider,
      String externalId,
      String owner,
      String name,
      String cloneUrl,
      String defaultBranch,
      boolean isPrivate,
      ImportStatus status,
      UUID registeredBy,
      Instant createdAt,
      Instant updatedAt,
      String statusDetail,
      long version) {
    return new Repository(
        id,
        organizationId,
        provider,
        externalId,
        owner,
        name,
        cloneUrl,
        defaultBranch,
        isPrivate,
        status,
        registeredBy,
        createdAt,
        updatedAt,
        statusDetail,
        version);
  }

  /** Marks the repository successfully indexed and READY. */
  public void markReady(Instant now) {
    this.status = ImportStatus.READY;
    this.statusDetail = null;
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  /** Marks the repository's import/indexing FAILED with a reason. */
  public void markFailed(String reason, Instant now) {
    this.status = ImportStatus.FAILED;
    this.statusDetail = reason;
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  /**
   * Requests a re-index: moves a terminal (READY/FAILED) repository back to IMPORTING so the worker
   * saga runs again. Only valid from a terminal state — re-indexing while an import is already in
   * flight is rejected to preserve the state machine.
   *
   * @throws IllegalStateException if a re-index is requested while not in a terminal state
   */
  public void requestReindex(Instant now) {
    if (status != ImportStatus.READY && status != ImportStatus.FAILED) {
      throw new IllegalStateException("Cannot re-index a repository that is still " + status);
    }
    this.status = ImportStatus.IMPORTING;
    this.statusDetail = null;
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  public RepositoryId id() {
    return id;
  }

  public UUID organizationId() {
    return organizationId;
  }

  public String provider() {
    return provider;
  }

  public String externalId() {
    return externalId;
  }

  public String owner() {
    return owner;
  }

  public String name() {
    return name;
  }

  public String cloneUrl() {
    return cloneUrl;
  }

  public String defaultBranch() {
    return defaultBranch;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public ImportStatus status() {
    return status;
  }

  public UUID registeredBy() {
    return registeredBy;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public String statusDetail() {
    return statusDetail;
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
    return o instanceof Repository other && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
