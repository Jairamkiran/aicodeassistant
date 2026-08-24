package com.jairam.aicodeassistant.integration.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Wires the GitHub integration: enables {@link GitHubProperties} and provides a {@link
 * RestClient.Builder} whose request factory enforces connect/read timeouts. Timeouts are the first
 * line of defence against a hung provider — an external call must never block a thread
 * indefinitely.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubIntegrationConfig {

  @Bean
  RestClient.Builder gitHubRestClientBuilder(GitHubProperties properties) {
    ClientHttpRequestFactorySettings settings =
        ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(orDefault(properties.connectTimeout(), Duration.ofSeconds(3)))
            .withReadTimeout(orDefault(properties.readTimeout(), Duration.ofSeconds(10)));
    // Pin HTTP/1.1 on the JDK client: avoids an HTTP/2 RST_STREAM edge case on
    // POST and matches GitHub's HTTP/1.1-friendly API.
    return RestClient.builder()
        .requestFactory(
            ClientHttpRequestFactoryBuilder.jdk()
                .withHttpClientCustomizer(b -> b.version(HttpClient.Version.HTTP_1_1))
                .build(settings));
  }

  private static Duration orDefault(Duration value, Duration fallback) {
    return value == null ? fallback : value;
  }
}
