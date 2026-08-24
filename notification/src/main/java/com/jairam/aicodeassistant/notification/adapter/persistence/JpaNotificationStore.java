package com.jairam.aicodeassistant.notification.adapter.persistence;

import com.jairam.aicodeassistant.notification.domain.Notification;
import com.jairam.aicodeassistant.notification.domain.NotificationStore;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** JPA-backed {@link NotificationStore}. */
@Component
class JpaNotificationStore implements NotificationStore {

  private final NotificationJpaRepository jpa;

  JpaNotificationStore(NotificationJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Notification save(Notification notification) {
    NotificationEntity entity =
        jpa.findById(notification.id())
            .map(
                existing -> {
                  existing.setRead(notification.isRead());
                  return existing;
                })
            .orElseGet(() -> toEntity(notification));
    return toDomain(jpa.save(entity));
  }

  @Override
  public Optional<Notification> findById(UUID id) {
    return jpa.findById(id).map(JpaNotificationStore::toDomain);
  }

  @Override
  public List<Notification> findByRecipient(UUID recipientUserId, int limit) {
    return jpa
        .findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId, PageRequest.of(0, limit))
        .stream()
        .map(JpaNotificationStore::toDomain)
        .toList();
  }

  @Override
  public long countUnread(UUID recipientUserId) {
    return jpa.countByRecipientUserIdAndReadFalse(recipientUserId);
  }

  private static NotificationEntity toEntity(Notification n) {
    return new NotificationEntity(
        n.id(),
        n.recipientUserId(),
        n.organizationId(),
        n.type(),
        n.title(),
        n.message(),
        n.resourceType(),
        n.resourceId(),
        n.createdAt(),
        n.isRead());
  }

  private static Notification toDomain(NotificationEntity e) {
    return Notification.rehydrate(
        e.getId(),
        e.getRecipientUserId(),
        e.getOrganizationId(),
        e.getType(),
        e.getTitle(),
        e.getMessage(),
        e.getResourceType(),
        e.getResourceId(),
        e.getCreatedAt(),
        e.isRead());
  }
}
