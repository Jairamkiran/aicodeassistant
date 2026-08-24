package com.jairam.aicodeassistant.notification.adapter.rest.dto;

import com.jairam.aicodeassistant.notification.domain.Notification;
import java.time.Instant;
import java.util.UUID;

/** API view of an in-app notification. */
public record NotificationView(
    UUID id,
    UUID organizationId,
    String type,
    String title,
    String message,
    String resourceType,
    String resourceId,
    Instant createdAt,
    boolean read) {

  public static NotificationView from(Notification n) {
    return new NotificationView(
        n.id(),
        n.organizationId(),
        n.type(),
        n.title(),
        n.message(),
        n.resourceType(),
        n.resourceId(),
        n.createdAt(),
        n.isRead());
  }
}
