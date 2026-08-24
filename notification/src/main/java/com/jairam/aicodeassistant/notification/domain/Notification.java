package com.jairam.aicodeassistant.notification.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * In-app notification aggregate: a message addressed to a user within an organization, with a
 * read/unread state. Created from a neutral {@code NotificationSignal} and surfaced via REST.
 */
public final class Notification {

  private final UUID id;
  private final UUID recipientUserId;
  private final UUID organizationId;
  private final String type;
  private final String title;
  private final String message;
  private final String resourceType;
  private final String resourceId;
  private final Instant createdAt;
  private boolean read;

  private Notification(
      UUID id,
      UUID recipientUserId,
      UUID organizationId,
      String type,
      String title,
      String message,
      String resourceType,
      String resourceId,
      Instant createdAt,
      boolean read) {
    this.id = Objects.requireNonNull(id, "id");
    this.recipientUserId = Objects.requireNonNull(recipientUserId, "recipientUserId");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.type = Objects.requireNonNull(type, "type");
    this.title = Objects.requireNonNull(title, "title");
    this.message = message == null ? "" : message;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.read = read;
  }

  /** Creates a new, unread notification. */
  public static Notification create(
      UUID recipientUserId,
      UUID organizationId,
      String type,
      String title,
      String message,
      String resourceType,
      String resourceId,
      Instant createdAt) {
    return new Notification(
        UUID.randomUUID(),
        recipientUserId,
        organizationId,
        type,
        title,
        message,
        resourceType,
        resourceId,
        createdAt,
        false);
  }

  /** Rehydrates a persisted notification. */
  public static Notification rehydrate(
      UUID id,
      UUID recipientUserId,
      UUID organizationId,
      String type,
      String title,
      String message,
      String resourceType,
      String resourceId,
      Instant createdAt,
      boolean read) {
    return new Notification(
        id,
        recipientUserId,
        organizationId,
        type,
        title,
        message,
        resourceType,
        resourceId,
        createdAt,
        read);
  }

  public void markRead() {
    this.read = true;
  }

  public UUID id() {
    return id;
  }

  public UUID recipientUserId() {
    return recipientUserId;
  }

  public UUID organizationId() {
    return organizationId;
  }

  public String type() {
    return type;
  }

  public String title() {
    return title;
  }

  public String message() {
    return message;
  }

  public String resourceType() {
    return resourceType;
  }

  public String resourceId() {
    return resourceId;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public boolean isRead() {
    return read;
  }
}
