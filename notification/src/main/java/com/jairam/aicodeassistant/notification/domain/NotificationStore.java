package com.jairam.aicodeassistant.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for persisting and querying {@link Notification}s. */
public interface NotificationStore {

  Notification save(Notification notification);

  Optional<Notification> findById(UUID id);

  /** A user's notifications, most recent first, capped at {@code limit}. */
  List<Notification> findByRecipient(UUID recipientUserId, int limit);

  /** Count of a user's unread notifications. */
  long countUnread(UUID recipientUserId);
}
