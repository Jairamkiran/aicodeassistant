package com.jairam.aicodeassistant.integration.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GitHub integration configuration, bound from {@code aicodeassistant.integration.github}.
 *
 * <p>The OAuth client id/secret identify our registered GitHub OAuth App and are secrets (supplied
 * via env/secret in real deployments; the dev fallback is non-functional against real GitHub and
 * clearly marked). Base URLs are overridable so tests can point the client at a WireMock server.
 *
 * @param clientId GitHub OAuth App client id
 * @param clientSecret GitHub OAuth App client secret
 * @param redirectUri our callback URL registered with the OAuth App
 * @param apiBaseUrl GitHub REST API base (default api.github.com)
 * @param authBaseUrl GitHub OAuth base (default github.com/login/oauth)
 * @param connectTimeout HTTP connect timeout
 * @param readTimeout HTTP read timeout
 */
@ConfigurationProperties(prefix = "aicodeassistant.integration.github")
public record GitHubProperties(
    String clientId,
    String clientSecret,
    String redirectUri,
    String apiBaseUrl,
    String authBaseUrl,
    Duration connectTimeout,
    Duration readTimeout) {

  public GitHubProperties {
    apiBaseUrl = orDefault(apiBaseUrl, "https://api.github.com");
    authBaseUrl = orDefault(authBaseUrl, "https://github.com/login/oauth");
    connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
    readTimeout = readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
  }

  private static String orDefault(String value, String fallback) {
    return (value == null || value.isBlank()) ? fallback : value;
  }
}
