package com.jairam.aicodeassistant.indexing.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for index-job persistence, including the atomic <em>claim</em> that gives
 * exactly-once processing without a distributed lock (see ADR-0009).
 */
public interface IndexJobStore {

  /** Persists a new or updated job. */
  IndexJob save(IndexJob job);

  Optional<IndexJob> findByRepositoryId(UUID repositoryId);

  /**
   * Atomically claims the job for a repository: transitions REGISTERED → CLAIMED via a conditional
   * update, returning the claimed job only if THIS caller won the claim. A concurrent worker (or a
   * job already past REGISTERED) yields {@link Optional#empty()}. This is the concurrency guard —
   * one conditional UPDATE, no lock, no TTL to misconfigure.
   *
   * @return the claimed job if won, else empty
   */
  Optional<IndexJob> claim(UUID repositoryId);

  /**
   * Fails jobs stuck in an in-progress state whose last update is older than {@code
   * staleAfterSeconds} (a worker crashed mid-saga). Returns the number reaped. Idempotent and
   * set-based, so it is safe to run from any/every worker instance.
   */
  int reapStalledJobs(long staleAfterSeconds);
}
