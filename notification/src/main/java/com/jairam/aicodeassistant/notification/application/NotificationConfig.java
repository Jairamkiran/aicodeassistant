package com.jairam.aicodeassistant.notification.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the default notification dispatcher unless a real (e.g. SMTP) one is provided. */
@Configuration(proxyBeanMethods = false)
class NotificationConfig {

  @Bean
  @ConditionalOnMissingBean(NotificationDispatcher.class)
  NotificationDispatcher loggingNotificationDispatcher() {
    return new LoggingNotificationDispatcher();
  }
}
