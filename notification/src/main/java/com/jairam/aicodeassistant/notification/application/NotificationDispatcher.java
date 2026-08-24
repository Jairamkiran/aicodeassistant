package com.jairam.aicodeassistant.notification.application;

import com.jairam.aicodeassistant.notification.domain.Notification;

/**
 * Out-of-band delivery channel for a persisted notification (e.g. email). Kept as a port so the
 * default log-based implementation can be swapped for a real SMTP sender without an SMTP dependency
 * baked into the core — no speculative provider abstraction, just one seam with one real consumer.
 */
public interface NotificationDispatcher {

  void dispatch(Notification notification);
}
