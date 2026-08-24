package com.jairam.aicodeassistant.notification.application;

import com.jairam.aicodeassistant.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link NotificationDispatcher}: logs the notification instead of sending an email. This
 * keeps the platform free of an SMTP dependency out of the box; a real email adapter can be added
 * as a bean and it takes precedence (see {@link NotificationConfig}).
 */
class LoggingNotificationDispatcher implements NotificationDispatcher {

  private static final Logger log = LoggerFactory.getLogger(LoggingNotificationDispatcher.class);

  @Override
  public void dispatch(Notification notification) {
    log.info(
        "notification[{}] to user {}: {} — {}",
        notification.type(),
        notification.recipientUserId(),
        notification.title(),
        notification.message());
  }
}
