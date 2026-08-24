package com.jairam.aicodeassistant.indexing.domain;

import com.jairam.aicodeassistant.platform.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/**
 * Published when a repository has been successfully indexed. Externalized to Kafka so the {@code
 * repository} context (in the app deployable) can move the repository to READY. Consumers are
 * idempotent on {@link #eventId()}.
 */
@Externalized("repository.indexed::#{#this.repositoryId()}")
public record RepositoryIndexed(UUID eventId, Instant occurredAt, UUID repositoryId, int chunkCount)
    implements DomainEvent {

  public static RepositoryIndexed of(UUID repositoryId, int chunkCount, Instant occurredAt) {
    return new RepositoryIndexed(UUID.randomUUID(), occurredAt, repositoryId, chunkCount);
  }
}
