package com.jairam.aicodeassistant.indexing.adapter.persistence;

import com.jairam.aicodeassistant.indexing.domain.IndexJob;
import com.jairam.aicodeassistant.indexing.domain.IndexJobStore;
import com.jairam.aicodeassistant.indexing.domain.IndexStatus;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** JPA-backed {@link IndexJobStore} with an atomic conditional-update claim. */
@Component
class JpaIndexJobStore implements IndexJobStore {

  private final IndexJobJpaRepository jpa;
  private final Clock clock;

  JpaIndexJobStore(IndexJobJpaRepository jpa, Clock clock) {
    this.jpa = jpa;
    this.clock = clock;
  }

  @Override
  public IndexJob save(IndexJob job) {
    return toDomain(jpa.save(toEntity(job)));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<IndexJob> findByRepositoryId(UUID repositoryId) {
    return jpa.findByRepositoryId(repositoryId).map(JpaIndexJobStore::toDomain);
  }

  @Override
  @Transactional
  public Optional<IndexJob> claim(UUID repositoryId) {
    int updated = jpa.claim(repositoryId, clock.instant());
    if (updated == 0) {
      return Optional.empty();
    }
    // We won the claim; reload the now-CLAIMED job.
    return jpa.findByRepositoryId(repositoryId).map(JpaIndexJobStore::toDomain);
  }

  @Override
  @Transactional
  public int reapStalledJobs(long staleAfterSeconds) {
    java.time.Instant now = clock.instant();
    return jpa.failStalledBefore(now.minusSeconds(staleAfterSeconds), now);
  }

  private static IndexJobEntity toEntity(IndexJob j) {
    return new IndexJobEntity(
        j.id(),
        j.repositoryId(),
        j.organizationId(),
        j.cloneUrl(),
        j.defaultBranch(),
        j.status().name(),
        j.attempts(),
        j.statusDetail(),
        j.chunkCount(),
        j.createdAt(),
        j.updatedAt(),
        j.version());
  }

  private static IndexJob toDomain(IndexJobEntity e) {
    return IndexJob.rehydrate(
        e.getId(),
        e.getRepositoryId(),
        e.getOrganizationId(),
        e.getCloneUrl(),
        e.getDefaultBranch(),
        IndexStatus.valueOf(e.getStatus()),
        e.getAttempts(),
        e.getStatusDetail(),
        e.getChunkCount(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getVersion());
  }
}
