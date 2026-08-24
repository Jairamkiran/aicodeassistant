package com.jairam.aicodeassistant.ai.embedding.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.jairam.aicodeassistant.ai.config.OllamaProperties;
import com.jairam.aicodeassistant.ai.embedding.EmbeddingException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

/**
 * Tests the Ollama embedding client against a WireMock server — real HTTP, no Ollama, no Docker.
 * Verifies success mapping and that 5xx/timeout become {@link EmbeddingException} (the retryable
 * domain error).
 */
class OllamaEmbeddingClientTest {

  private WireMockServer wireMock;
  private OllamaEmbeddingClient client;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    wireMock.start();
    com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", wireMock.port());
    String base = "http://localhost:" + wireMock.port();
    var props =
        new OllamaProperties(
            base,
            "nomic-embed-text",
            3,
            "llama3.1",
            Duration.ofMillis(500),
            Duration.ofMillis(800));
    var settings =
        ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(Duration.ofMillis(500))
            .withReadTimeout(Duration.ofMillis(800));
    RestClient http =
        RestClient.builder()
            .baseUrl(base)
            .requestFactory(
                ClientHttpRequestFactoryBuilder.jdk()
                    .withHttpClientCustomizer(b -> b.version(HttpClient.Version.HTTP_1_1))
                    .build(settings))
            .build();
    client = new OllamaEmbeddingClient(http, props, new SimpleMeterRegistry());
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Test
  void embedsBatchAndMapsVectors() {
    stubFor(
        post(urlPathEqualTo("/api/embed"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"embeddings\":[[0.1,0.2,0.3],[0.4,0.5,0.6]]}")));

    List<float[]> vectors = client.embedAll(List.of("alpha", "beta"));

    assertThat(vectors).hasSize(2);
    assertThat(vectors.get(0)).containsExactly(0.1f, 0.2f, 0.3f);
    assertThat(vectors.get(1)).containsExactly(0.4f, 0.5f, 0.6f);
  }

  @Test
  void emptyInputShortCircuits() {
    assertThat(client.embedAll(List.of())).isEmpty();
  }

  @Test
  void serverErrorBecomesEmbeddingException() {
    stubFor(post(urlPathEqualTo("/api/embed")).willReturn(aResponse().withStatus(500)));
    assertThatThrownBy(() -> client.embedAll(List.of("x"))).isInstanceOf(EmbeddingException.class);
  }

  @Test
  void timeoutBecomesEmbeddingException() {
    stubFor(
        post(urlPathEqualTo("/api/embed"))
            .willReturn(aResponse().withStatus(200).withFixedDelay(2000).withBody("{}")));
    assertThatThrownBy(() -> client.embedAll(List.of("x"))).isInstanceOf(EmbeddingException.class);
  }

  @Test
  void countMismatchBecomesEmbeddingException() {
    stubFor(
        post(urlPathEqualTo("/api/embed"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"embeddings\":[[0.1,0.2,0.3]]}"))); // 1 vector for 2 inputs

    assertThatThrownBy(() -> client.embedAll(List.of("a", "b")))
        .isInstanceOf(EmbeddingException.class);
  }
}
