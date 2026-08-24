package com.jairam.aicodeassistant.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.jairam.aicodeassistant.ai.chat.ChatModel;
import com.jairam.aicodeassistant.ai.chat.ChatRequest;
import com.jairam.aicodeassistant.ai.chat.ChatResponse;
import com.jairam.aicodeassistant.ai.chat.ChatToken;
import com.jairam.aicodeassistant.ai.chat.TokenUsage;
import com.jairam.aicodeassistant.conversation.domain.ChatSession;
import com.jairam.aicodeassistant.conversation.domain.ChatSessionStore;
import com.jairam.aicodeassistant.conversation.domain.ChatTurn;
import com.jairam.aicodeassistant.retrieval.search.CodeSearch;
import com.jairam.aicodeassistant.retrieval.search.SearchQuery;
import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the RAG orchestrator with in-memory fakes for every collaborator — no DB, no search engine,
 * no LLM. Verifies: citations are derived from retrieved chunks; both turns are persisted; windowed
 * memory is fed on a follow-up turn; and the streaming path assembles text and persists on
 * completion.
 */
class RagChatServiceTest {

  private static final UUID ORG = UUID.randomUUID();
  private static final UUID USER = UUID.randomUUID();
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

  private FakeSessionStore store;
  private FakeCodeSearch search;
  private RecordingChatModel chat;
  private RagChatService service;
  private UUID sessionId;

  @BeforeEach
  void setUp() {
    store = new FakeSessionStore();
    search = new FakeCodeSearch();
    chat = new RecordingChatModel();
    service =
        new RagChatService(
            store,
            search,
            chat,
            new ConversationProperties(8, 6, 12_000),
            new SimpleMeterRegistry(),
            clock);
    ChatSession session = ChatSession.start(ORG, null, USER, "s", clock.instant());
    store.save(session);
    sessionId = session.id();
  }

  private SearchResult hit(String path) {
    return new SearchResult(
        UUID.randomUUID(),
        UUID.randomUUID(),
        path,
        "java",
        1,
        5,
        "code in " + path,
        0.9,
        SearchResult.Source.HYBRID);
  }

  @Test
  void blockingAskReturnsAnswerWithCitationsFromRetrievalAndPersistsTurns() {
    search.results = List.of(hit("Auth.java"), hit("Jwt.java"));
    chat.reply = "It uses JWT [1][2].";

    RagChatService.Answer answer = service.ask(sessionId, "how does auth work?");

    assertThat(answer.content()).isEqualTo("It uses JWT [1][2].");
    // Citations come from the retrieved chunks, not parsed from the reply.
    assertThat(answer.citations()).hasSize(2);
    assertThat(answer.citations().get(0).filePath()).isEqualTo("Auth.java");

    // Both the user turn and the assistant turn are persisted, in order.
    ChatSession reloaded = store.findById(sessionId).orElseThrow();
    List<ChatTurn> turns = reloaded.turns();
    assertThat(turns).hasSize(2);
    assertThat(turns.get(0).role()).isEqualTo(ChatTurn.Role.USER);
    assertThat(turns.get(1).role()).isEqualTo(ChatTurn.Role.ASSISTANT);
    assertThat(turns.get(1).citations()).hasSize(2);
  }

  @Test
  void searchIsScopedToTheSessionsOrganization() {
    search.results = List.of(hit("A.java"));
    service.ask(sessionId, "q");
    assertThat(search.lastQuery.organizationId()).isEqualTo(ORG);
  }

  @Test
  void followUpTurnFeedsPriorTurnsAsMemory() {
    search.results = List.of(hit("A.java"));
    chat.reply = "first answer";
    service.ask(sessionId, "first question");

    chat.reply = "second answer";
    service.ask(sessionId, "second question");

    // On the 2nd turn the prompt must include the earlier turns (memory).
    List<String> lastPromptContents =
        chat.lastRequest.messages().stream().map(m -> m.content()).toList();
    assertThat(lastPromptContents).anyMatch(c -> c.contains("first question"));
    assertThat(lastPromptContents).anyMatch(c -> c.contains("first answer"));
    assertThat(lastPromptContents).anyMatch(c -> c.equals("second question"));
  }

  @Test
  void streamingAssemblesTokensAndPersistsAssistantTurn() {
    search.results = List.of(hit("A.java"));
    chat.streamTokens =
        List.of(ChatToken.delta("Hel"), ChatToken.delta("lo"), ChatToken.done(TokenUsage.UNKNOWN));

    RagChatService.StreamingAnswer answer = service.askStream(sessionId, "hi");

    StringBuilder full = new StringBuilder();
    try (Stream<ChatToken> tokens = answer.tokens()) {
      tokens.forEach(t -> full.append(t.delta()));
    }
    assertThat(full.toString()).isEqualTo("Hello");

    // Caller signals completion → assistant turn persisted with citations.
    answer.onComplete().accept(full.toString());
    ChatSession reloaded = store.findById(sessionId).orElseThrow();
    ChatTurn assistant = reloaded.turns().get(reloaded.turns().size() - 1);
    assertThat(assistant.role()).isEqualTo(ChatTurn.Role.ASSISTANT);
    assertThat(assistant.content()).isEqualTo("Hello");
    assertThat(assistant.citations()).hasSize(1);
  }

  // --- Fakes -----------------------------------------------------------------

  static final class FakeSessionStore implements ChatSessionStore {
    private final Map<UUID, ChatSession> byId = new HashMap<>();

    @Override
    public ChatSession save(ChatSession session) {
      byId.put(session.id(), session);
      return session;
    }

    @Override
    public Optional<ChatSession> findById(UUID sessionId) {
      return Optional.ofNullable(byId.get(sessionId));
    }

    @Override
    public void deleteById(UUID sessionId) {
      byId.remove(sessionId);
    }

    @Override
    public List<ChatSession> findByUserAndOrganization(UUID userId, UUID organizationId) {
      return new ArrayList<>(byId.values());
    }
  }

  static final class FakeCodeSearch implements CodeSearch {
    List<SearchResult> results = List.of();
    SearchQuery lastQuery;

    @Override
    public List<SearchResult> search(SearchQuery query) {
      this.lastQuery = query;
      return results;
    }
  }

  static final class RecordingChatModel implements ChatModel {
    String reply = "answer";
    List<ChatToken> streamTokens = List.of();
    ChatRequest lastRequest;

    @Override
    public ChatResponse chat(ChatRequest request) {
      this.lastRequest = request;
      return new ChatResponse(reply, TokenUsage.UNKNOWN, "stop");
    }

    @Override
    public Stream<ChatToken> chatStream(ChatRequest request) {
      this.lastRequest = request;
      return streamTokens.stream();
    }

    @Override
    public String provider() {
      return "fake";
    }
  }
}
