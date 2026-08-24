package com.jairam.aicodeassistant.platform.notification;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A neutral, cross-cutting notification signal published by any module and consumed by the
 * notification context — mirroring the {@link
 * com.jairam.aicodeassistant.platform.audit.AuditSignal} pattern.
 *
 * <p>Defining it in the shared kernel keeps the notification module coupled only to {@code
 * platform}: publishers emit a {@code NotificationSignal}, notification subscribes to exactly this
 * one type and never reaches into a producer's internals. Unlike an audit signal, a notification is
 * addressed to a specific recipient user within an organization.
 *
 * @param recipientUserId the user who should see the notification
 * @param organizationId the organization the notification relates to
 * @param type stable type key, e.g. {@code "REPOSITORY_INDEXED"}, {@code
 *     "REPOSITORY_INDEXING_FAILED"}
 * @param title short headline
 * @param message human-readable body
 * @param resourceType type of the related resource (nullable), e.g. {@code "REPOSITORY"}
 * @param resourceId id of the related resource (nullable)
 * @param occurredAt when the underlying event happened
 */
public record NotificationSignal(
    UUID recipientUserId,
    UUID organizationId,
    String type,
    String title,
    String message,
    String resourceType,
    String resourceId,
    Instant occurredAt) {

  public NotificationSignal {
    Objects.requireNonNull(recipientUserId, "recipientUserId");
    Objects.requireNonNull(organizationId, "organizationId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(occurredAt, "occurredAt");
    message = message == null ? "" : message;
  }

  public static Builder builder(String type) {
    return new Builder(type);
  }

  /** Fluent builder for readable call sites. */
  public static final class Builder {
    private final String type;
    private UUID recipientUserId;
    private UUID organizationId;
    private String title;
    private String message = "";
    private String resourceType;
    private String resourceId;
    private Instant occurredAt;

    private Builder(String type) {
      this.type = type;
    }

    public Builder recipient(UUID userId) {
      this.recipientUserId = userId;
      return this;
    }

    public Builder organization(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder resource(String resourceType, String resourceId) {
      this.resourceType = resourceType;
      this.resourceId = resourceId;
      return this;
    }

    public Builder occurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    public NotificationSignal build() {
      return new NotificationSignal(
          recipientUserId,
          organizationId,
          type,
          title,
          message,
          resourceType,
          resourceId,
          occurredAt);
    }
  }
}
