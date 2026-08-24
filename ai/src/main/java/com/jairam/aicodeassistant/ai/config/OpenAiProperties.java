package com.jairam.aicodeassistant.ai.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAI chat configuration, bound from {@code aicodeassistant.ai.openai}. Only used when {@code
 * aicodeassistant.ai.chat.provider=openai}.
 *
 * @param baseUrl API base (default api.openai.com/v1); overridable for tests/proxies
 * @param apiKey secret bearer key (supplied via env/secret; empty by default)
 * @param chatModel default chat model (e.g. gpt-4o-mini)
 * @param connectTimeout HTTP connect timeout
 * @param readTimeout HTTP read timeout
 */
@ConfigurationProperties(prefix = "aicodeassistant.ai.openai")
public record OpenAiProperties(
    String baseUrl,
    String apiKey,
    String chatModel,
    Duration connectTimeout,
    Duration readTimeout) {

  public OpenAiProperties {
    baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://api.openai.com/v1" : baseUrl;
    apiKey = apiKey == null ? "" : apiKey;
    chatModel = (chatModel == null || chatModel.isBlank()) ? "gpt-4o-mini" : chatModel;
    connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
    readTimeout = readTimeout == null ? Duration.ofSeconds(60) : readTimeout;
  }
}
