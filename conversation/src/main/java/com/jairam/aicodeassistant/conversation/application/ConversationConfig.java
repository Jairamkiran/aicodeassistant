package com.jairam.aicodeassistant.conversation.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables conversation/RAG tuning properties. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConversationProperties.class)
public class ConversationConfig {}
