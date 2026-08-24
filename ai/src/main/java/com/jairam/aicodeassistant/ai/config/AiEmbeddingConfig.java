package com.jairam.aicodeassistant.ai.config;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Wires the Ollama embedding client: enables {@link OllamaProperties} and builds a base-URL'd
 * {@link RestClient} with enforced timeouts (embeddings can be slow on CPU, hence a longer read
 * timeout).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OllamaProperties.class)
public class AiEmbeddingConfig {

  @Bean
  RestClient ollamaRestClient(OllamaProperties properties) {
    ClientHttpRequestFactorySettings settings =
        ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(properties.connectTimeout())
            .withReadTimeout(properties.readTimeout());
    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .requestFactory(
            ClientHttpRequestFactoryBuilder.jdk()
                .withHttpClientCustomizer(b -> b.version(HttpClient.Version.HTTP_1_1))
                .build(settings))
        .build();
  }
}
