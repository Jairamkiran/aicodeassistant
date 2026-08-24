package com.jairam.aicodeassistant.codeintel.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.jairam.aicodeassistant.ai.chat.ChatMessage;
import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewPromptBuilderTest {

  private static SearchResult hit(String path, int start, int end, String content) {
    return new SearchResult(
        UUID.randomUUID(),
        UUID.randomUUID(),
        path,
        "java",
        start,
        end,
        content,
        1.0,
        SearchResult.Source.HYBRID);
  }

  @Test
  void fencesContextAsUntrustedDataAndRequestsJson() {
    List<ChatMessage> messages =
        new ReviewPromptBuilder(10_000)
            .build("null-safety", List.of(hit("A.java", 1, 3, "class A {}")));

    ChatMessage system = messages.get(0);
    assertThat(system.role()).isEqualTo(ChatMessage.Role.SYSTEM);
    assertThat(system.content())
        .contains("<context>")
        .contains("</context>")
        .contains("untrusted repository")
        .contains("SINGLE valid JSON")
        .contains("A.java:1-3");

    ChatMessage user = messages.get(1);
    assertThat(user.role()).isEqualTo(ChatMessage.Role.USER);
    assertThat(user.content()).contains("null-safety");
  }

  @Test
  void respectsContextCharBudgetButKeepsAtLeastOneBlock() {
    // Budget smaller than a single block: still include the first block.
    List<ChatMessage> messages =
        new ReviewPromptBuilder(1)
            .build(
                "focus",
                List.of(
                    hit("A.java", 1, 100, "x".repeat(500)),
                    hit("B.java", 1, 100, "y".repeat(500))));

    String system = messages.get(0).content();
    assertThat(system).contains("A.java");
    assertThat(system).doesNotContain("B.java");
  }

  @Test
  void handlesNoRetrievedSnippets() {
    List<ChatMessage> messages = new ReviewPromptBuilder(10_000).build("focus", List.of());
    assertThat(messages.get(0).content()).contains("(no relevant snippets found)");
  }
}
