package com.jairam.aicodeassistant.platform.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker contract for all domain events published across the system.
 *
 * <p>Every event carries a unique {@link #eventId()} (for idempotent consumers and de-duplication)
 * and an {@link #occurredAt()} timestamp (for ordering and audit). Events are published in-process
 * through Spring's {@code ApplicationEventPublisher}; those annotated for externalization are
 * relayed to Kafka via the transactional outbox (Spring Modulith's JPA event publication registry),
 * guaranteeing at-least-once delivery without a dual write.
 *
 * <p>Implementations should be immutable records living in the PUBLIC api package of their owning
 * bounded context — they are part of the context's published contract, unlike internal aggregates.
 */
public interface DomainEvent {

  /** Globally unique id for this event occurrence; stable across retries/relays. */
  UUID eventId();

  /** Wall-clock instant the event occurred, per the application {@code Clock}. */
  Instant occurredAt();

  /**
   * Logical event type name used for routing/serialisation (defaults to the simple class name).
   * Kept stable even if the class is later moved/renamed.
   */
  default String eventType() {
    return getClass().getSimpleName();
  }
}
