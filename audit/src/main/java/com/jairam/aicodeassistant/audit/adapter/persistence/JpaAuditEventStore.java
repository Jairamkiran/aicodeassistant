package com.jairam.aicodeassistant.audit.adapter.persistence;

import com.jairam.aicodeassistant.audit.domain.AuditEvent;
import com.jairam.aicodeassistant.audit.domain.AuditEventStore;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed {@link AuditEventStore}. Maps the immutable event to an entity and inserts it. */
@Component
class JpaAuditEventStore implements AuditEventStore {

  private final AuditEventJpaRepository jpa;

  JpaAuditEventStore(AuditEventJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public void append(AuditEvent e) {
    jpa.save(
        new AuditEventEntity(
            UUID.randomUUID(),
            e.occurredAt(),
            e.eventType(),
            e.outcome().name(),
            e.actorType().name(),
            e.actorId(),
            e.targetType(),
            e.targetId(),
            e.correlationId(),
            e.clientIp(),
            e.detail()));
  }
}
