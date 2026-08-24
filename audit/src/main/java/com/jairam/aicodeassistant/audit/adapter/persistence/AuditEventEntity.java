package com.jairam.aicodeassistant.audit.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the append-only {@code audit_events} table. No version column and no setters
 * beyond construction: rows are inserted once and never mutated.
 */
@Entity
@Table(name = "audit_events")
class AuditEventEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  @Column(name = "event_type", nullable = false, updatable = false)
  private String eventType;

  @Column(nullable = false, updatable = false)
  private String outcome;

  @Column(name = "actor_type", nullable = false, updatable = false)
  private String actorType;

  @Column(name = "actor_id", updatable = false)
  private String actorId;

  @Column(name = "target_type", updatable = false)
  private String targetType;

  @Column(name = "target_id", updatable = false)
  private String targetId;

  @Column(name = "correlation_id", updatable = false)
  private String correlationId;

  @Column(name = "client_ip", updatable = false)
  private String clientIp;

  @Column(updatable = false)
  private String detail;

  protected AuditEventEntity() {
    // Required by JPA.
  }

  AuditEventEntity(
      UUID id,
      Instant occurredAt,
      String eventType,
      String outcome,
      String actorType,
      String actorId,
      String targetType,
      String targetId,
      String correlationId,
      String clientIp,
      String detail) {
    this.id = id;
    this.occurredAt = occurredAt;
    this.eventType = eventType;
    this.outcome = outcome;
    this.actorType = actorType;
    this.actorId = actorId;
    this.targetType = targetType;
    this.targetId = targetId;
    this.correlationId = correlationId;
    this.clientIp = clientIp;
    this.detail = detail;
  }

  String getEventType() {
    return eventType;
  }

  String getOutcome() {
    return outcome;
  }

  String getActorType() {
    return actorType;
  }

  String getActorId() {
    return actorId;
  }
}
