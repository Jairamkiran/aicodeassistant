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
import com.jairam.aicodeassistant.ai.config.OllamaProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the Ollama chat client against WireMock — real HTTP, no Ollama. Covers blocking success +
 * usage mapping, NDJSON streaming assembly, and 5xx → domain exception. No Docker.
 */
class OllamaChatModelTest {

  private WireMockServer wireMock;
  private OllamaChatModel model;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    wireMock.start();
    com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", wireMock.port());
    var props =
        new OllamaProperties(
            "http://localhost:" + wireMock.port(),
            "nomic-embed-text",
            768,
            "llama3.1",
            Duration.ofMillis(500),
            Duration.ofMillis(1500));
    model = new OllamaChatModel(props, new ObjectMapper(), new SimpleMeterRegistry());
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
        post(urlPathEqualTo("/api/chat"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"hi there\"},"
                            + "\"done\":true,\"done_reason\":\"stop\","
                            + "\"prompt_eval_count\":11,\"eval_count\":5}")));

    ChatResponse response = model.chat(request());

    assertThat(response.content()).isEqualTo("hi there");
    assertThat(response.usage().promptTokens()).isEqualTo(11);
    assertThat(response.usage().completionTokens()).isEqualTo(5);
    assertThat(response.finishReason()).isEqualTo("stop");
  }

  @Test
  void streamingAssemblesDeltasAndDone() {
    // NDJSON: one object per line; final line has done:true.
    String ndjson =
        "{\"message\":{\"content\":\"Hel\"},\"done\":false}\n"
            + "{\"message\":{\"content\":\"lo\"},\"done\":false}\n"
            + "{\"message\":{\"content\":\"\"},\"done\":true,\"eval_count\":2,\"prompt_eval_count\":3}\n";
    stubFor(
        post(urlPathEqualTo("/api/chat")).willReturn(aResponse().withStatus(200).withBody(ndjson)));

    StringBuilder text = new StringBuilder();
    boolean[] sawDone = {false};
    try (Stream<ChatToken> stream = model.chatStream(request())) {
      stream.forEach(
          t -> {
            text.append(t.delta());
            if (t.done()) {
              sawDone[0] = true;
              assertThat(t.usage().completionTokens()).isEqualTo(2);
            }
          });
    }

    assertThat(text.toString()).isEqualTo("Hello");
    assertThat(sawDone[0]).isTrue();
  }

  @Test
  void serverErrorBecomesChatException() {
    stubFor(post(urlPathEqualTo("/api/chat")).willReturn(aResponse().withStatus(500)));
    assertThatThrownBy(() -> model.chat(request())).isInstanceOf(ChatException.Unavailable.class);
  }
}
