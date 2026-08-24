package com.jairam.aicodeassistant.indexing.domain;

import com.jairam.aicodeassistant.platform.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/**
 * Published when indexing a repository failed (after the saga's compensation). Externalized to
 * Kafka so the {@code repository} context can move the repository to FAILED and surface the reason.
 * Idempotent on {@link #eventId()}.
 */
@Externalized("repository.indexing-failed::#{#this.repositoryId()}")
public record RepositoryIndexingFailed(
    UUID eventId, Instant occurredAt, UUID repositoryId, String reason) implements DomainEvent {

  public static RepositoryIndexingFailed of(UUID repositoryId, String reason, Instant occurredAt) {
    return new RepositoryIndexingFailed(UUID.randomUUID(), occurredAt, repositoryId, reason);
  }
}
