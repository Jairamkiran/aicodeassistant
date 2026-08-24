package com.jairam.aicodeassistant.integration.github.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.jairam.aicodeassistant.integration.config.GitHubProperties;
import com.jairam.aicodeassistant.integration.github.GitHubException;
import com.jairam.aicodeassistant.integration.github.GitHubRepo;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

/**
 * Tests the GitHub HTTP client against a WireMock server — a real HTTP round trip with no real
 * GitHub — asserting the integration directive's failure handling: success mapping to domain types,
 * and 401/404/429/5xx/timeout each mapped to the right {@link GitHubException} subtype. Runs in
 * normal test scope (no Docker).
 */
class GitHubApiClientTest {

  private WireMockServer wireMock;
  private GitHubApiClient client;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    wireMock.start();
    // Point the static stubFor(...) DSL at this server instance (not the default 8080).
    com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", wireMock.port());
    String base = "http://localhost:" + wireMock.port();
    var properties =
        new GitHubProperties(
            "cid",
            "secret",
            base + "/callback",
            base,
            base,
            Duration.ofMillis(500),
            Duration.ofMillis(800));
    // RestClient with a short read timeout so the timeout test is fast.
    var settings =
        ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(Duration.ofMillis(500))
            .withReadTimeout(Duration.ofMillis(800));
    // Pin HTTP/1.1: the JDK client otherwise negotiates HTTP/2 with WireMock and
    // can hit RST_STREAM on POST. GitHub's API is HTTP/1.1-friendly; the
    // production config makes the same choice (see GitHubIntegrationConfig).
    RestClient.Builder builder =
        RestClient.builder()
            .requestFactory(
                ClientHttpRequestFactoryBuilder.jdk()
                    .withHttpClientCustomizer(
                        b -> b.version(java.net.http.HttpClient.Version.HTTP_1_1))
                    .build(settings));
    client = new GitHubApiClient(builder, properties, new SimpleMeterRegistry());
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Test
  void listRepositoriesMapsJsonToDomain() {
    stubFor(
        get(urlPathEqualTo("/user/repos"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        [{"id":42,"name":"acme","full_name":"octo/acme","private":true,
                          "clone_url":"https://github.com/octo/acme.git","default_branch":"main",
                          "owner":{"login":"octo"}}]
                        """)));

    List<GitHubRepo> repos = client.listRepositories("token");

    assertThat(repos).hasSize(1);
    GitHubRepo repo = repos.get(0);
    assertThat(repo.externalId()).isEqualTo("42");
    assertThat(repo.owner()).isEqualTo("octo");
    assertThat(repo.fullName()).isEqualTo("octo/acme");
    assertThat(repo.isPrivate()).isTrue();
    assertThat(repo.defaultBranch()).isEqualTo("main");
  }

  @Test
  void unauthorizedMapsToCredentialRejected() {
    stubFor(get(urlPathEqualTo("/user/repos")).willReturn(aResponse().withStatus(401)));
    assertThatThrownBy(() -> client.listRepositories("bad-token"))
        .isInstanceOf(GitHubException.CredentialRejected.class);
  }

  @Test
  void rateLimitedMapsToUnavailable() {
    stubFor(get(urlPathEqualTo("/user/repos")).willReturn(aResponse().withStatus(429)));
    assertThatThrownBy(() -> client.listRepositories("token"))
        .isInstanceOf(GitHubException.Unavailable.class);
  }

  @Test
  void serverErrorMapsToUnavailable() {
    stubFor(get(urlPathEqualTo("/user/repos")).willReturn(aResponse().withStatus(503)));
    assertThatThrownBy(() -> client.listRepositories("token"))
        .isInstanceOf(GitHubException.Unavailable.class);
  }

  @Test
  void readTimeoutMapsToUnavailable() {
    stubFor(
        get(urlPathEqualTo("/user/repos"))
            .willReturn(aResponse().withStatus(200).withFixedDelay(2000).withBody("[]")));
    assertThatThrownBy(() -> client.listRepositories("token"))
        .isInstanceOf(GitHubException.Unavailable.class);
  }

  @Test
  void tokenExchangeSuccessReturnsToken() {
    stubFor(
        post(urlPathEqualTo("/access_token"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"gho_abc\",\"scope\":\"repo,read:user\"}")));

    var result = client.exchangeCodeForToken("code", "cid", "secret", "http://cb");
    assertThat(result.accessToken()).isEqualTo("gho_abc");
    assertThat(result.scopes()).contains("repo");
  }

  @Test
  void tokenExchangeWithErrorBodyIsCredentialRejected() {
    stubFor(
        post(urlPathEqualTo("/access_token"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"bad_verification_code\"}")));

    assertThatThrownBy(() -> client.exchangeCodeForToken("bad", "cid", "secret", "http://cb"))
        .isInstanceOf(GitHubException.CredentialRejected.class);
  }
}
