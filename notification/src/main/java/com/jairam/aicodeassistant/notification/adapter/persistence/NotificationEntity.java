package com.jairam.aicodeassistant.notification.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** JPA entity for the {@code notifications} table. */
@Entity
@Table(name = "notifications")
class NotificationEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "recipient_user_id", nullable = false, updatable = false)
  private UUID recipientUserId;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(nullable = false, updatable = false)
  private String type;

  @Column(nullable = false, updatable = false)
  private String title;

  @Column(updatable = false)
  private String message;

  @Column(name = "resource_type", updatable = false)
  private String resourceType;

  @Column(name = "resource_id", updatable = false)
  private String resourceId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  protected NotificationEntity() {
    // Required by JPA.
  }

  NotificationEntity(
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
    this.id = id;
    this.recipientUserId = recipientUserId;
    this.organizationId = organizationId;
    this.type = type;
    this.title = title;
    this.message = message;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.createdAt = createdAt;
    this.read = read;
  }

  UUID getId() {
    return id;
  }

  UUID getRecipientUserId() {
    return recipientUserId;
  }

  UUID getOrganizationId() {
    return organizationId;
  }

  String getType() {
    return type;
  }

  String getTitle() {
    return title;
  }

  String getMessage() {
    return message;
  }

  String getResourceType() {
    return resourceType;
  }

  String getResourceId() {
    return resourceId;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  boolean isRead() {
    return read;
  }

  void setRead(boolean read) {
    this.read = read;
  }
}
