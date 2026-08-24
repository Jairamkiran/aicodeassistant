package com.jairam.aicodeassistant.indexing.application;

import com.jairam.aicodeassistant.indexing.domain.IndexJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled reaper that fails index jobs stuck in an in-progress state — the follow-up to the
 * atomic-claim design (ADR-0009). The claim guarantees exactly-one worker starts a job, but if that
 * worker crashes mid-saga the job would sit in CLAIMED/CLONING/… forever. This reaper marks such
 * jobs FAILED once they are older than a staleness threshold, so they surface as failures (and can
 * be re-indexed) instead of hanging.
 *
 * <p>Idempotent and set-based, so running it on every worker instance is safe.
 */
@Component
class StalledJobReaper {

  private static final Logger log = LoggerFactory.getLogger(StalledJobReaper.class);

  private final IndexJobStore jobs;
  private final long staleAfterSeconds;

  StalledJobReaper(
      IndexJobStore jobs,
      @Value("${aicodeassistant.indexing.reaper.stale-after-seconds:900}") long staleAfterSeconds) {
    this.jobs = jobs;
    this.staleAfterSeconds = staleAfterSeconds;
  }

  @Scheduled(
      initialDelayString = "${aicodeassistant.indexing.reaper.initial-delay-ms:120000}",
      fixedDelayString = "${aicodeassistant.indexing.reaper.interval-ms:300000}")
  void reap() {
    int reaped = jobs.reapStalledJobs(staleAfterSeconds);
    if (reaped > 0) {
      log.warn("Reaped {} stalled index job(s) older than {}s", reaped, staleAfterSeconds);
    }
  }
}
