package com.jairam.aicodeassistant.ai.embedding.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jairam.aicodeassistant.ai.config.OllamaProperties;
import com.jairam.aicodeassistant.ai.embedding.EmbeddingClient;
import com.jairam.aicodeassistant.ai.embedding.EmbeddingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Ollama-backed {@link EmbeddingClient} using the {@code POST /api/embed} batch endpoint. Provider
 * JSON DTOs are private to this class; callers get plain {@code float[]}.
 *
 * <p>Integration directive: connect/read timeouts on the client, Resilience4j {@code @Retry} +
 * {@code @CircuitBreaker} (backend {@code ollama}) around the call, per-call Micrometer timer, and
 * WARN logs on failure. Any transport/5xx failure becomes an {@link EmbeddingException} — no
 * provider exception leaks.
 */
@Component
public class OllamaEmbeddingClient implements EmbeddingClient {

  private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingClient.class);
  static final String BACKEND = "ollama";

  private final RestClient http;
  private final OllamaProperties properties;
  private final MeterRegistry metrics;

  OllamaEmbeddingClient(
      RestClient ollamaRestClient, OllamaProperties properties, MeterRegistry metrics) {
    this.http = ollamaRestClient;
    this.properties = properties;
    this.metrics = metrics;
  }

  @Override
  public int dimension() {
    return properties.dimension();
  }

  @Override
  @CircuitBreaker(name = BACKEND)
  @Retry(name = BACKEND)
  public List<float[]> embedAll(List<String> texts) {
    if (texts == null || texts.isEmpty()) {
      return List.of();
    }
    Timer.Sample sample = Timer.start(metrics);
    try {
      EmbedResponse response =
          http.post()
              .uri("/api/embed")
              .contentType(MediaType.APPLICATION_JSON)
              .body(new EmbedRequest(properties.embeddingModel(), texts))
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  (req, res) -> {
                    throw new EmbeddingException("status " + res.getStatusCode().value());
                  })
              .body(EmbedResponse.class);

      if (response == null || response.embeddings() == null) {
        throw new EmbeddingException("empty embeddings response");
      }
      if (response.embeddings().size() != texts.size()) {
        throw new EmbeddingException(
            "embedding count mismatch: expected "
                + texts.size()
                + " got "
                + response.embeddings().size());
      }
      record(sample, "success");
      return response.embeddings();
    } catch (EmbeddingException e) {
      record(sample, "failure");
      throw e;
    } catch (RestClientException e) {
      record(sample, "error");
      log.warn("Ollama embedding transport error: {}", e.getMessage());
      throw new EmbeddingException(e.getMessage());
    }
  }

  private void record(Timer.Sample sample, String outcome) {
    sample.stop(
        Timer.builder("ai.embedding.call")
            .tag("provider", BACKEND)
            .tag("outcome", outcome)
            .register(metrics));
  }

  /** Ollama /api/embed request. Package-private DTO. */
  record EmbedRequest(String model, List<String> input) {}

  /** Ollama /api/embed response. {@code embeddings} is a list of vectors. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record EmbedResponse(List<float[]> embeddings) {}
}
