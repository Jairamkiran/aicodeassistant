package com.jairam.aicodeassistant.repository.domain;

import com.jairam.aicodeassistant.platform.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/**
 * Published when a repository is registered and its import is requested. This is the trigger the
 * indexing worker (M4) consumes to run the clone→parse→embed saga.
 *
 * <p>Annotated {@link Externalized} so Spring Modulith relays it to Kafka via the transactional
 * outbox (the {@code event_publication} table) — persisted in the same transaction as the
 * repository row, then delivered at-least-once. Consumers must be idempotent on {@link #eventId()}.
 *
 * @param eventId unique id for this occurrence
 * @param occurredAt when the import was requested
 * @param repositoryId the registered repository
 * @param organizationId owning organization
 * @param cloneUrl HTTPS clone URL for the worker
 * @param defaultBranch branch to index
 */
@Externalized("repository.import-requested::#{#this.repositoryId()}")
public record RepositoryImportRequested(
    UUID eventId,
    Instant occurredAt,
    UUID repositoryId,
    UUID organizationId,
    String cloneUrl,
    String defaultBranch)
    implements DomainEvent {

  public static RepositoryImportRequested of(
      UUID repositoryId,
      UUID organizationId,
      String cloneUrl,
      String defaultBranch,
      Instant occurredAt) {
    return new RepositoryImportRequested(
        UUID.randomUUID(), occurredAt, repositoryId, organizationId, cloneUrl, defaultBranch);
  }
}
