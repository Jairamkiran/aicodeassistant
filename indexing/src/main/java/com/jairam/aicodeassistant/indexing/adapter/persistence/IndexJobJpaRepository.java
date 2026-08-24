package com.jairam.aicodeassistant.indexing.adapter.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link IndexJobEntity}, incl. the atomic claim. */
interface IndexJobJpaRepository extends JpaRepository<IndexJobEntity, UUID> {

  Optional<IndexJobEntity> findByRepositoryId(UUID repositoryId);

  /**
   * Atomic claim: flips REGISTERED → CLAIMED for exactly one caller. Returns the number of rows
   * updated (1 = claim won, 0 = someone else has it / not claimable). This conditional UPDATE is
   * the concurrency guard (ADR-0009).
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update IndexJobEntity j set j.status = 'CLAIMED', j.updatedAt = :now "
          + "where j.repositoryId = :repositoryId and j.status = 'REGISTERED'")
  int claim(@Param("repositoryId") UUID repositoryId, @Param("now") Instant now);

  /**
   * Fails jobs stuck in a non-terminal, in-progress state (a worker died mid-saga) whose last
   * update predates {@code cutoff}. Returns the number reaped. Set-based and idempotent — safe to
   * run from every worker instance.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update IndexJobEntity j set j.status = 'FAILED', "
          + "j.statusDetail = 'Reaped: stalled in-progress job', j.updatedAt = :now "
          + "where j.updatedAt < :cutoff "
          + "and j.status in ('CLAIMED', 'CLONING', 'PARSING', 'EMBEDDING', 'UPSERTING')")
  int failStalledBefore(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
