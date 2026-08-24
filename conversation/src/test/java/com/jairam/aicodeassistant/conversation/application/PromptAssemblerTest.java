package com.jairam.aicodeassistant.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.jairam.aicodeassistant.ai.chat.ChatMessage;
import com.jairam.aicodeassistant.conversation.domain.ChatTurn;
import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the RAG prompt assembler — pure logic, no I/O. Verifies the injection guardrail is
 * present, chunks are numbered, citations are derived from the included chunks (not text), the
 * character budget drops the lowest-ranked chunks, and windowed prior turns are included.
 */
class PromptAssemblerTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  private static SearchResult hit(String path, int startLine, String content) {
    return new SearchResult(
        UUID.randomUUID(),
        UUID.randomUUID(),
        path,
        "java",
        startLine,
        startLine + 5,
        content,
        0.9,
        SearchResult.Source.HYBRID);
  }

  @Test
  void systemMessageContainsGuardrailAndNumberedContext() {
    var assembler = new PromptAssembler(12_000);
    var prompt =
        assembler.assemble(
            "how does auth work?",
            List.of(hit("Auth.java", 10, "class Auth {}"), hit("Jwt.java", 20, "class Jwt {}")),
            List.of());

    ChatMessage system = prompt.messages().get(0);
    assertThat(system.role()).isEqualTo(ChatMessage.Role.SYSTEM);
    // Guardrail: untrusted data, not instructions.
    assertThat(system.content()).containsIgnoringCase("untrusted");
    assertThat(system.content()).contains("<context>").contains("</context>");
    // Numbered snippets with file:line headers.
    assertThat(system.content()).contains("[1] Auth.java:10-15").contains("[2] Jwt.java:20-25");
  }

  @Test
  void citationsAreDerivedFromIncludedChunks() {
    var assembler = new PromptAssembler(12_000);
    var h1 = hit("A.java", 1, "a");
    var h2 = hit("B.java", 2, "b");
    var prompt = assembler.assemble("q", List.of(h1, h2), List.of());

    assertThat(prompt.citations()).hasSize(2);
    assertThat(prompt.citations().get(0).index()).isEqualTo(1);
    assertThat(prompt.citations().get(0).chunkId()).isEqualTo(h1.chunkId());
    assertThat(prompt.citations().get(0).filePath()).isEqualTo("A.java");
    assertThat(prompt.citations().get(1).chunkId()).isEqualTo(h2.chunkId());
  }

  @Test
  void characterBudgetDropsLowestRankedChunks() {
    // Tiny budget: only the first chunk fits (at least one is always kept).
    var assembler = new PromptAssembler(80);
    var prompt =
        assembler.assemble(
            "q",
            List.of(
                hit("First.java", 1, "x".repeat(60)),
                hit("Second.java", 2, "y".repeat(60)),
                hit("Third.java", 3, "z".repeat(60))),
            List.of());

    assertThat(prompt.citations()).hasSize(1);
    assertThat(prompt.citations().get(0).filePath()).isEqualTo("First.java");
    assertThat(prompt.messages().get(0).content()).doesNotContain("Second.java");
  }

  @Test
  void priorTurnsAreIncludedInOrderBeforeTheQuestion() {
    var assembler = new PromptAssembler(12_000);
    List<ChatTurn> prior =
        List.of(
            ChatTurn.user(0, "earlier question", NOW),
            ChatTurn.assistant(1, "earlier answer", List.of(), NOW));

    var prompt = assembler.assemble("follow up", List.of(hit("A.java", 1, "a")), prior);

    List<ChatMessage> messages = prompt.messages();
    // [0]=system, [1]=prior user, [2]=prior assistant, [3]=new question
    assertThat(messages).hasSize(4);
    assertThat(messages.get(1).content()).isEqualTo("earlier question");
    assertThat(messages.get(2).role()).isEqualTo(ChatMessage.Role.ASSISTANT);
    assertThat(messages.get(3).content()).isEqualTo("follow up");
  }

  @Test
  void handlesNoRetrievedContextGracefully() {
    var assembler = new PromptAssembler(12_000);
    var prompt = assembler.assemble("q", List.of(), List.of());
    assertThat(prompt.citations()).isEmpty();
    assertThat(prompt.messages().get(0).content()).contains("no relevant snippets");
  }
}
