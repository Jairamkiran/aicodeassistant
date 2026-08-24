package com.jairam.aicodeassistant.platform.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A neutral, cross-cutting audit signal published by any module and consumed by the audit context.
 *
 * <p>Defining it in the shared kernel (rather than having audit depend on every module's event
 * types) keeps the audit module coupled only to {@code platform}: publishers emit {@code
 * AuditSignal}, audit subscribes to exactly this one type. This is the simplest design that
 * preserves module boundaries — chosen over a fan of per-module listeners.
 *
 * @param action stable action name, e.g. {@code "USER_LOGIN"}, {@code "API_KEY_CREATED"}
 * @param success whether the audited action succeeded
 * @param actorType who acted: {@code "USER"}, {@code "API_KEY"}, {@code "ANONYMOUS"}, {@code
 *     "SYSTEM"}
 * @param actorId identifier of the actor (nullable)
 * @param targetType type of affected resource (nullable)
 * @param targetId id of affected resource (nullable)
 * @param occurredAt when it happened
 * @param attributes small map of extra non-sensitive context (never secrets)
 */
public record AuditSignal(
    String action,
    boolean success,
    String actorType,
    String actorId,
    String targetType,
    String targetId,
    Instant occurredAt,
    Map<String, String> attributes) {

  public AuditSignal {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(actorType, "actorType");
    Objects.requireNonNull(occurredAt, "occurredAt");
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }

  public static Builder builder(String action) {
    return new Builder(action);
  }

  /** Fluent builder for readable call sites. */
  public static final class Builder {
    private final String action;
    private boolean success = true;
    private String actorType = "ANONYMOUS";
    private String actorId;
    private String targetType;
    private String targetId;
    private Instant occurredAt;
    private Map<String, String> attributes = Map.of();

    private Builder(String action) {
      this.action = action;
    }

    public Builder success(boolean v) {
      this.success = v;
      return this;
    }

    public Builder actor(String type, String id) {
      this.actorType = type;
      this.actorId = id;
      return this;
    }

    public Builder target(String type, String id) {
      this.targetType = type;
      this.targetId = id;
      return this;
    }

    public Builder occurredAt(Instant v) {
      this.occurredAt = v;
      return this;
    }

    public Builder attributes(Map<String, String> v) {
      this.attributes = v;
      return this;
    }

    public AuditSignal build() {
      return new AuditSignal(
          action, success, actorType, actorId, targetType, targetId, occurredAt, attributes);
    }
  }
}
