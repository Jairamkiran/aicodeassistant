package com.jairam.aicodeassistant.ai.chat.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairam.aicodeassistant.ai.chat.ChatException;
import com.jairam.aicodeassistant.ai.chat.ChatMessage;
import com.jairam.aicodeassistant.ai.chat.ChatModel;
import com.jairam.aicodeassistant.ai.chat.ChatRequest;
import com.jairam.aicodeassistant.ai.chat.ChatResponse;
import com.jairam.aicodeassistant.ai.chat.ChatToken;
import com.jairam.aicodeassistant.ai.chat.TokenUsage;
import com.jairam.aicodeassistant.ai.config.OpenAiProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
 * OpenAI-backed {@link ChatModel} using {@code POST /v1/chat/completions}. Active when {@code
 * aicodeassistant.ai.chat.provider=openai}.
 *
 * <p>Blocking mode maps {@code choices[0].message.content} + {@code usage}. Streaming mode consumes
 * Server-Sent Events: lines prefixed {@code data: }, each a JSON chunk with {@code
 * choices[0].delta.content}, terminated by a literal {@code data: [DONE]}. Provider DTOs are
 * private; {@code 401/403} → {@link ChatException.CredentialRejected} (not retried), transient →
 * {@link ChatException.Unavailable}.
 */
@Component
@ConditionalOnProperty(name = "aicodeassistant.ai.chat.provider", havingValue = "openai")
public class OpenAiChatModel implements ChatModel {

  private static final Logger log = LoggerFactory.getLogger(OpenAiChatModel.class);
  static final String BACKEND = "openai-chat";
  static final String PROVIDER = "openai";
  private static final String DONE_SENTINEL = "[DONE]";

  private final HttpClient httpClient;
  private final OpenAiProperties properties;
  private final ObjectMapper mapper;
  private final MeterRegistry metrics;

  OpenAiChatModel(OpenAiProperties properties, ObjectMapper mapper, MeterRegistry metrics) {
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
    HttpResponse<String> response = send(body, HttpResponse.BodyHandlers.ofString());
    try {
      OpenAiResponse parsed = mapper.readValue(response.body(), OpenAiResponse.class);
      String content =
          parsed.choices() == null || parsed.choices().isEmpty()
              ? ""
              : parsed.choices().get(0).message().content();
      TokenUsage usage =
          parsed.usage() == null
              ? TokenUsage.UNKNOWN
              : new TokenUsage(parsed.usage().promptTokens(), parsed.usage().completionTokens());
      recordTokens(usage);
      String finish =
          parsed.choices() == null || parsed.choices().isEmpty()
              ? null
              : parsed.choices().get(0).finishReason();
      return new ChatResponse(content == null ? "" : content, usage, finish);
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
        send(body, HttpResponse.BodyHandlers.ofInputStream());
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
    return reader
        .lines()
        .map(this::parseSseLine)
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

  /** Parses one SSE line; returns null for non-data/keepalive lines. */
  private ChatToken parseSseLine(String line) {
    if (line == null || !line.startsWith("data:")) {
      return null;
    }
    String payload = line.substring("data:".length()).trim();
    if (payload.isEmpty()) {
      return null;
    }
    if (DONE_SENTINEL.equals(payload)) {
      return ChatToken.done(TokenUsage.UNKNOWN);
    }
    try {
      OpenAiStreamChunk chunk = mapper.readValue(payload, OpenAiStreamChunk.class);
      if (chunk.choices() == null || chunk.choices().isEmpty()) {
        return null;
      }
      OpenAiStreamChunk.Delta delta = chunk.choices().get(0).delta();
      String text = delta == null ? null : delta.content();
      return text == null ? null : ChatToken.delta(text);
    } catch (Exception e) {
      log.warn("Skipping unparseable OpenAI SSE line: {}", e.getMessage());
      return null;
    }
  }

  private <T> HttpResponse<T> send(String body, HttpResponse.BodyHandler<T> handler) {
    Timer.Sample sample = Timer.start(metrics);
    try {
      HttpRequest httpRequest =
          HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/chat/completions"))
              .timeout(properties.readTimeout())
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + properties.apiKey())
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<T> response = httpClient.send(httpRequest, handler);
      int code = response.statusCode();
      if (code == 401 || code == 403) {
        recordCall(sample, "failure");
        throw new ChatException.CredentialRejected(PROVIDER);
      }
      if (code >= 400) {
        recordCall(sample, "failure");
        throw new ChatException.Unavailable(PROVIDER, "status " + code);
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
      log.warn("OpenAI chat transport error: {}", e.getMessage());
      throw new ChatException.Unavailable(PROVIDER, e.getMessage());
    }
  }

  private String requestJson(ChatRequest request, boolean stream) {
    try {
      List<OpenAiMessage> messages =
          request.messages().stream()
              .map(m -> new OpenAiMessage(role(m.role()), m.content()))
              .toList();
      return mapper.writeValueAsString(
          new OpenAiRequest(
              request.model() == null ? properties.chatModel() : request.model(),
              messages,
              stream,
              request.temperature(),
              request.maxTokens()));
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

  private void recordCall(Timer.Sample sample, String outcome) {
    sample.stop(
        Timer.builder("ai.chat.call")
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

  // --- OpenAI JSON DTOs (package-private; never leak) --------------------------
  record OpenAiRequest(
      String model,
      List<OpenAiMessage> messages,
      boolean stream,
      Double temperature,
      @JsonProperty("max_tokens") Integer maxTokens) {}

  record OpenAiMessage(String role, String content) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record OpenAiResponse(List<Choice> choices, Usage usage) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(OpenAiMessage message, @JsonProperty("finish_reason") String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("completion_tokens") int completionTokens) {}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record OpenAiStreamChunk(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Delta delta, @JsonProperty("finish_reason") String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Delta(String content) {}
  }
}
