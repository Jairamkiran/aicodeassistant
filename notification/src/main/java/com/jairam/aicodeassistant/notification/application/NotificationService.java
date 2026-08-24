package com.jairam.aicodeassistant.notification.application;

import com.jairam.aicodeassistant.notification.domain.Notification;
import com.jairam.aicodeassistant.notification.domain.NotificationStore;
import com.jairam.aicodeassistant.platform.error.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for in-app notifications: create (from a signal), list, unread count, mark read. A
 * created notification is also dispatched to an out-of-band {@link NotificationDispatcher} (email
 * by default logs; a real SMTP adapter can replace it) so the two channels stay decoupled.
 */
@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
  private static final int MAX_LIST = 100;

  private final NotificationStore store;
  private final NotificationDispatcher dispatcher;

  public NotificationService(NotificationStore store, NotificationDispatcher dispatcher) {
    this.store = store;
    this.dispatcher = dispatcher;
  }

  /** Persists a new notification and dispatches it out-of-band. */
  @Transactional
  public Notification create(Notification notification) {
    Notification saved = store.save(notification);
    dispatcher.dispatch(saved);
    log.debug(
        "Created notification {} for user {} ({})",
        saved.id(),
        saved.recipientUserId(),
        saved.type());
    return saved;
  }

  /** Lists a user's notifications, most recent first. */
  @Transactional(readOnly = true)
  public List<Notification> list(UUID userId, int limit) {
    int effective = limit <= 0 || limit > MAX_LIST ? 20 : limit;
    return store.findByRecipient(userId, effective);
  }

  /** Number of unread notifications for a user. */
  @Transactional(readOnly = true)
  public long unreadCount(UUID userId) {
    return store.countUnread(userId);
  }

  /**
   * Marks a notification read. The caller must own it, otherwise it is treated as not found (does
   * not reveal another user's notification).
   */
  @Transactional
  public void markRead(UUID userId, UUID notificationId) {
    Notification notification =
        store
            .findById(notificationId)
            .filter(n -> n.recipientUserId().equals(userId))
            .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
    notification.markRead();
    store.save(notification);
  }
}
