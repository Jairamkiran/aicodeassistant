package com.jairam.aicodeassistant.repository.application;

import com.jairam.aicodeassistant.platform.audit.AuditSignal;
import com.jairam.aicodeassistant.platform.notification.NotificationSignal;
import com.jairam.aicodeassistant.repository.domain.Repository;
import com.jairam.aicodeassistant.repository.domain.RepositoryId;
import com.jairam.aicodeassistant.repository.domain.RepositoryStore;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies indexing outcomes to the repository aggregate: READY on success, FAILED (with reason)
 * otherwise. Called by the Kafka listener that consumes the worker's completion events. Idempotent
 * — re-applying the same terminal status is harmless.
 */
@Service
public class RepositoryStatusService {

  private static final Logger log = LoggerFactory.getLogger(RepositoryStatusService.class);

  private final RepositoryStore repositories;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public RepositoryStatusService(
      RepositoryStore repositories, ApplicationEventPublisher events, Clock clock) {
    this.repositories = repositories;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public void markIndexed(UUID repositoryId, int chunkCount) {
    repositories
        .findById(new RepositoryId(repositoryId))
        .ifPresent(
            repo -> {
              repo.markReady(clock.instant());
              repositories.save(repo);
              audit(repo, "REPOSITORY_INDEXED", true);
              notify(
                  repo,
                  "REPOSITORY_INDEXED",
                  "Repository ready",
                  "\"" + repo.name() + "\" finished indexing and is ready to search and chat.");
              log.info("Repository {} is READY ({} chunks)", repositoryId, chunkCount);
            });
  }

  @Transactional
  public void markFailed(UUID repositoryId, String reason) {
    repositories
        .findById(new RepositoryId(repositoryId))
        .ifPresent(
            repo -> {
              repo.markFailed(reason, clock.instant());
              repositories.save(repo);
              audit(repo, "REPOSITORY_INDEXING_FAILED", false);
              notify(
                  repo,
                  "REPOSITORY_INDEXING_FAILED",
                  "Repository indexing failed",
                  "Indexing \"" + repo.name() + "\" failed: " + reason);
              log.warn("Repository {} indexing FAILED: {}", repositoryId, reason);
            });
  }

  private void audit(Repository repo, String action, boolean success) {
    events.publishEvent(
        AuditSignal.builder(action)
            .success(success)
            .actor("SYSTEM", null)
            .target("REPOSITORY", repo.id().toString())
            .occurredAt(clock.instant())
            .build());
  }

  /** Notifies the user who registered the repository of a terminal indexing outcome. */
  private void notify(Repository repo, String type, String title, String message) {
    events.publishEvent(
        NotificationSignal.builder(type)
            .recipient(repo.registeredBy())
            .organization(repo.organizationId())
            .title(title)
            .message(message)
            .resource("REPOSITORY", repo.id().toString())
            .occurredAt(clock.instant())
            .build());
  }
}
