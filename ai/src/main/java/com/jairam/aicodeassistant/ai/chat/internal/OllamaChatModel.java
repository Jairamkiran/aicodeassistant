package com.jairam.aicodeassistant.ai.chat.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairam.aicodeassistant.ai.chat.ChatException;
import com.jairam.aicodeassistant.ai.chat.ChatMessage;
import com.jairam.aicodeassistant.ai.chat.ChatModel;
import com.jairam.aicodeassistant.ai.chat.ChatRequest;
import com.jairam.aicodeassistant.ai.chat.ChatResponse;
import com.jairam.aicodeassistant.ai.chat.ChatToken;
import com.jairam.aicodeassistant.ai.chat.TokenUsage;
import com.jairam.aicodeassistant.ai.config.OllamaProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ollama-backed {@link ChatModel} (the offline default) using {@code POST /api/chat}. Active when
 * {@code aicodeassistant.ai.chat.provider} is {@code ollama} or unset.
 *
 * <p>Blocking mode sends {@code stream:false} and maps one JSON response. Streaming mode sends
 * {@code stream:true}; Ollama replies with newline-delimited JSON (NDJSON), one object per token,
 * the last carrying {@code done:true} plus eval counts — parsed line-by-line into {@link
 * ChatToken}s. Provider DTOs are private; failures become {@link ChatException}. Uses the JDK
 * {@link HttpClient} directly so the streaming body is a real {@link java.io.InputStream}.
 */
@Component
@ConditionalOnProperty(
    name = "aicodeassistant.ai.chat.provider",
    havingValue = "ollama",
    matchIfMissing = true)
public class OllamaChatModel implements ChatModel {

  private static final Logger log = LoggerFactory.getLogger(OllamaChatModel.class);
  static final String BACKEND = "ollama-chat";
  static final String PROVIDER = "ollama";

  private final HttpClient httpClient;
  private final OllamaProperties properties;
  private final ObjectMapper mapper;
  private final MeterRegistry metrics;

  OllamaChatModel(OllamaProperties properties, ObjectMapper mapper, MeterRegistry metrics) {
    this.properties = properties;
    this.mapper = mapper;
    this.metrics = metrics;
    this.httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(properties.connectTimeout())
            .build();
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  @CircuitBreaker(name = BACKEND)
  @Retry(name = BACKEND)
  public ChatResponse chat(ChatRequest request) {
    String body = requestJson(request, false);
    HttpResponse<String> response = send(request, body, HttpResponse.BodyHandlers.ofString());
    try {
      OllamaChatResponse parsed = mapper.readValue(response.body(), OllamaChatResponse.class);
      TokenUsage usage =
          new TokenUsage(
              parsed.promptEvalCount() == null ? 0 : parsed.promptEvalCount(),
              parsed.evalCount() == null ? 0 : parsed.evalCount());
      recordTokens(usage);
      String content = parsed.message() == null ? "" : parsed.message().content();
      return new ChatResponse(content == null ? "" : content, usage, parsed.doneReason());
    } catch (ChatException e) {
      throw e;
    } catch (Exception e) {
      throw new ChatException.Unavailable(PROVIDER, "unparseable response: " + e.getMessage());
    }
  }

  @Override
  @CircuitBreaker(name = BACKEND)
  @Retry(name = BACKEND)
  public Stream<ChatToken> chatStream(ChatRequest request) {
    String body = requestJson(request, true);
    HttpResponse<java.io.InputStream> response =
        send(request, body, HttpResponse.BodyHandlers.ofInputStream());
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
    // Each NDJSON line → a ChatToken; close the reader (and thus the socket) when done.
    return reader
        .lines()
        .map(this::parseStreamLine)
        .filter(java.util.Objects::nonNull)
        .onClose(
            () -> {
              try {
                reader.close();
              } catch (Exception ignored) {
                // best-effort close
              }
            });
  }

  private ChatToken parseStreamLine(String line) {
    if (line == null || line.isBlank()) {
      return null;
    }
    try {
      OllamaChatResponse chunk = mapper.readValue(line, OllamaChatResponse.class);
      if (Boolean.TRUE.equals(chunk.done())) {
        TokenUsage usage =
            new TokenUsage(
                chunk.promptEvalCount() == null ? 0 : chunk.promptEvalCount(),
                chunk.evalCount() == null ? 0 : chunk.evalCount());
        recordTokens(usage);
        return ChatToken.done(usage);
      }
      String delta = chunk.message() == null ? "" : chunk.message().content();
      return ChatToken.delta(delta == null ? "" : delta);
    } catch (Exception e) {
      log.warn("Skipping unparseable Ollama stream line: {}", e.getMessage());
      return null;
    }
  }

  private <T> HttpResponse<T> send(
      ChatRequest request, String body, HttpResponse.BodyHandler<T> handler) {
    var sample = io.micrometer.core.instrument.Timer.start(metrics);
    try {
      HttpRequest httpRequest =
          HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/api/chat"))
              .timeout(properties.readTimeout())
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<T> response = httpClient.send(httpRequest, handler);
      if (response.statusCode() >= 400) {
        recordCall(sample, "failure");
        throw new ChatException.Unavailable(PROVIDER, "status " + response.statusCode());
      }
      recordCall(sample, "success");
      return response;
    } catch (ChatException e) {
      throw e;
    } catch (java.io.IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      recordCall(sample, "error");
      log.warn("Ollama chat transport error: {}", e.getMessage());
      throw new ChatException.Unavailable(PROVIDER, e.getMessage());
    }
  }

  private String requestJson(ChatRequest request, boolean stream) {
    try {
      List<OllamaMessage> messages =
          request.messages().stream()
              .map(m -> new OllamaMessage(role(m.role()), m.content()))
              .toList();
      OllamaOptions options =
          request.temperature() == null && request.maxTokens() == null
              ? null
              : new OllamaOptions(request.temperature(), request.maxTokens());
      return mapper.writeValueAsString(
          new OllamaChatRequest(
              request.model() == null ? properties.chatModel() : request.model(),
              messages,
              stream,
              options));
    } catch (Exception e) {
      throw new ChatException.Unavailable(PROVIDER, "could not serialise request");
    }
  }

  private static String role(ChatMessage.Role role) {
    return switch (role) {
      case SYSTEM -> "system";
      case USER -> "user";
      case ASSISTANT -> "assistant";
    };
  }

  private void recordCall(io.micrometer.core.instrument.Timer.Sample sample, String outcome) {
    sample.stop(
        io.micrometer.core.instrument.Timer.builder("ai.chat.call")
            .tag("provider", PROVIDER)
            .tag("outcome", outcome)
            .register(metrics));
  }

  private void recordTokens(TokenUsage usage) {
    metrics
        .counter("ai.chat.tokens", "provider", PROVIDER, "kind", "prompt")
        .increment(usage.promptTokens());
    metrics
        .counter("ai.chat.tokens", "provider", PROVIDER, "kind", "completion")
        .increment(usage.completionTokens());
  }

  // --- Ollama JSON DTOs (package-private; never leak) --------------------------
  record OllamaChatRequest(
      String model, List<OllamaMessage> messages, boolean stream, OllamaOptions options) {}

  record OllamaMessage(String role, String content) {}

  record OllamaOptions(Double temperature, Integer num_predict) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record OllamaChatResponse(
      OllamaMessage message,
      Boolean done,
      @com.fasterxml.jackson.annotation.JsonProperty("done_reason") String doneReason,
      @com.fasterxml.jackson.annotation.JsonProperty("prompt_eval_count") Integer promptEvalCount,
      @com.fasterxml.jackson.annotation.JsonProperty("eval_count") Integer evalCount) {}
}
