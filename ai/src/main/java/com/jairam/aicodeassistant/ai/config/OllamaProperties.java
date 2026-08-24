package com.jairam.aicodeassistant.ai.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ollama configuration, bound from {@code aicodeassistant.ai.ollama}. Covers both the embedding
 * model (M4) and the chat model (M6).
 *
 * @param baseUrl Ollama server base URL
 * @param embeddingModel embedding model name (default nomic-embed-text)
 * @param dimension embedding dimension the model produces (must match the DB column)
 * @param chatModel chat model name (default llama3.1)
 * @param connectTimeout HTTP connect timeout
 * @param readTimeout HTTP read timeout (generation can be slow on CPU)
 */
@ConfigurationProperties(prefix = "aicodeassistant.ai.ollama")
public record OllamaProperties(
    String baseUrl,
    String embeddingModel,
    int dimension,
    String chatModel,
    Duration connectTimeout,
    Duration readTimeout) {

  public OllamaProperties {
    baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "http://localhost:11434" : baseUrl;
    embeddingModel =
        (embeddingModel == null || embeddingModel.isBlank()) ? "nomic-embed-text" : embeddingModel;
    dimension = dimension <= 0 ? 768 : dimension;
    chatModel = (chatModel == null || chatModel.isBlank()) ? "llama3.1" : chatModel;
    connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
    readTimeout = readTimeout == null ? Duration.ofSeconds(60) : readTimeout;
  }
}
