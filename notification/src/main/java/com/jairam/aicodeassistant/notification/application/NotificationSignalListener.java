package com.jairam.aicodeassistant.notification.application;

import com.jairam.aicodeassistant.notification.domain.Notification;
import com.jairam.aicodeassistant.platform.notification.NotificationSignal;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges neutral {@link NotificationSignal}s published anywhere in the system into persisted
 * in-app notifications. This is the notification module's only inbound coupling — to the shared
 * kernel's signal type, never to another context's internals (mirrors the audit module).
 */
@Component
class NotificationSignalListener {

  private final NotificationService service;

  NotificationSignalListener(NotificationService service) {
    this.service = service;
  }

  @EventListener
  void on(NotificationSignal signal) {
    service.create(
        Notification.create(
            signal.recipientUserId(),
            signal.organizationId(),
            signal.type(),
            signal.title(),
            signal.message(),
            signal.resourceType(),
            signal.resourceId(),
            signal.occurredAt()));
  }
}
