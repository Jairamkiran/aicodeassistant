package com.jairam.aicodeassistant.audit.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * An immutable audit record. Built via {@link #builder()} because most fields are optional context
 * (actor/target/ip/detail) around a required core (type + outcome + time).
 *
 * @param eventType stable action name, e.g. {@code "USER_LOGIN"}
 * @param outcome success or failure
 * @param occurredAt when it happened (application clock)
 * @param actorType who acted
 * @param actorId identifier of the actor (nullable for anonymous)
 * @param targetType type of the affected resource (nullable)
 * @param targetId id of the affected resource (nullable)
 * @param correlationId request correlation id for cross-referencing logs/traces
 * @param clientIp originating client IP (nullable)
 * @param detail short human-readable context (nullable; never secrets)
 */
public record AuditEvent(
    String eventType,
    AuditOutcome outcome,
    Instant occurredAt,
    ActorType actorType,
    String actorId,
    String targetType,
    String targetId,
    String correlationId,
    String clientIp,
    String detail) {

  public AuditEvent {
    Objects.requireNonNull(eventType, "eventType");
    Objects.requireNonNull(outcome, "outcome");
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(actorType, "actorType");
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Fluent builder — keeps call sites readable given the many optional fields. */
  public static final class Builder {
    private String eventType;
    private AuditOutcome outcome = AuditOutcome.SUCCESS;
    private Instant occurredAt;
    private ActorType actorType = ActorType.ANONYMOUS;
    private String actorId;
    private String targetType;
    private String targetId;
    private String correlationId;
    private String clientIp;
    private String detail;

    public Builder eventType(String v) {
      this.eventType = v;
      return this;
    }

    public Builder outcome(AuditOutcome v) {
      this.outcome = v;
      return this;
    }

    public Builder occurredAt(Instant v) {
      this.occurredAt = v;
      return this;
    }

    public Builder actor(ActorType type, String id) {
      this.actorType = type;
      this.actorId = id;
      return this;
    }

    public Builder target(String type, String id) {
      this.targetType = type;
      this.targetId = id;
      return this;
    }

    public Builder correlationId(String v) {
      this.correlationId = v;
      return this;
    }

    public Builder clientIp(String v) {
      this.clientIp = v;
      return this;
    }

    public Builder detail(String v) {
      this.detail = v;
      return this;
    }

    public AuditEvent build() {
      return new AuditEvent(
          eventType,
          outcome,
          occurredAt,
          actorType,
          actorId,
          targetType,
          targetId,
          correlationId,
          clientIp,
          detail);
    }
  }
}
