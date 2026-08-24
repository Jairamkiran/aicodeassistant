package com.jairam.aicodeassistant.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables OpenAI chat configuration. {@link OllamaProperties} is already enabled by {@code
 * AiEmbeddingConfig} (shared by the embedding + Ollama-chat clients). The active {@link
 * com.jairam.aicodeassistant.ai.chat.ChatModel} implementation is chosen by
 * {@code @ConditionalOnProperty} on the adapters (Ollama default, OpenAI when {@code
 * aicodeassistant.ai.chat.provider=openai}).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenAiProperties.class)
public class AiChatConfig {}
