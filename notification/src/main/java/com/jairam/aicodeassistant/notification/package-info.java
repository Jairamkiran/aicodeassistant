/**
 * Notification bounded context.
 *
 * <p>Spring Modulith application module providing in-app notifications. It subscribes to neutral
 * {@link com.jairam.aicodeassistant.platform.notification.NotificationSignal}s published by other
 * modules (via Spring's event mechanism) and never invokes their internals, so it stays decoupled
 * and independently extractable — mirroring the audit module. Delivery beyond the in-app inbox goes
 * through a {@code NotificationDispatcher} port (logging by default; an SMTP adapter can replace it
 * with no core dependency on email).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Notification")
package com.jairam.aicodeassistant.notification;
