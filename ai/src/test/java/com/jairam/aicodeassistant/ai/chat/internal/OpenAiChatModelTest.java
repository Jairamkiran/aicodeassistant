package com.jairam.aicodeassistant.ai.chat.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.jairam.aicodeassistant.ai.chat.ChatException;
import com.jairam.aicodeassistant.ai.chat.ChatMessage;
import com.jairam.aicodeassistant.ai.chat.ChatRequest;
import com.jairam.aicodeassistant.ai.chat.ChatResponse;
import com.jairam.aicodeassistant.ai.chat.ChatToken;
import com.jairam.aicodeassistant.ai.config.OpenAiProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the OpenAI chat client against WireMock — real HTTP, no OpenAI. Covers blocking success +
 * usage mapping, SSE streaming assembly ({@code data:} lines + {@code [DONE]}), and 401 →
 * CredentialRejected (the non-retryable path). No Docker.
 */
class OpenAiChatModelTest {

  private WireMockServer wireMock;
  private OpenAiChatModel model;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    wireMock.start();
    com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", wireMock.port());
    var props =
        new OpenAiProperties(
            "http://localhost:" + wireMock.port() + "/v1",
            "test-key",
            "gpt-4o-mini",
            Duration.ofMillis(500),
            Duration.ofMillis(1500));
    model = new OpenAiChatModel(props, new ObjectMapper(), new SimpleMeterRegistry());
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  private static ChatRequest request() {
    return ChatRequest.of(List.of(ChatMessage.user("hello")));
  }

  @Test
  void blockingChatMapsContentAndUsage() {
    stubFor(
        post(urlPathEqualTo("/v1/chat/completions"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"hi\"},"
                            + "\"finish_reason\":\"stop\"}],"
                            + "\"usage\":{\"prompt_tokens\":7,\"completion_tokens\":2}}")));

    ChatResponse response = model.chat(request());

    assertThat(response.content()).isEqualTo("hi");
    assertThat(response.usage().promptTokens()).isEqualTo(7);
    assertThat(response.usage().completionTokens()).isEqualTo(2);
    assertThat(response.finishReason()).isEqualTo("stop");
  }

  @Test
  void streamingAssemblesSseDeltas() {
    String sse =
        "data: {\"choices\":[{\"delta\":{\"content\":\"Hel\"}}]}\n\n"
            + "data: {\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}\n\n"
            + "data: [DONE]\n\n";
    stubFor(
        post(urlPathEqualTo("/v1/chat/completions"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/event-stream")
                    .withBody(sse)));

    StringBuilder text = new StringBuilder();
    boolean[] sawDone = {false};
    try (Stream<ChatToken> stream = model.chatStream(request())) {
      stream.forEach(
          t -> {
            text.append(t.delta());
            if (t.done()) {
              sawDone[0] = true;
            }
          });
    }

    assertThat(text.toString()).isEqualTo("Hello");
    assertThat(sawDone[0]).isTrue();
  }

  @Test
  void unauthorizedBecomesCredentialRejected() {
    stubFor(post(urlPathEqualTo("/v1/chat/completions")).willReturn(aResponse().withStatus(401)));
    assertThatThrownBy(() -> model.chat(request()))
        .isInstanceOf(ChatException.CredentialRejected.class);
  }

  @Test
  void serverErrorBecomesUnavailable() {
    stubFor(post(urlPathEqualTo("/v1/chat/completions")).willReturn(aResponse().withStatus(503)));
    assertThatThrownBy(() -> model.chat(request())).isInstanceOf(ChatException.Unavailable.class);
  }
}
