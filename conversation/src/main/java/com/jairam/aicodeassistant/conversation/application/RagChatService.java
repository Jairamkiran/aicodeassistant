package com.jairam.aicodeassistant.conversation.application;

import com.jairam.aicodeassistant.ai.chat.ChatModel;
import com.jairam.aicodeassistant.ai.chat.ChatRequest;
import com.jairam.aicodeassistant.ai.chat.ChatResponse;
import com.jairam.aicodeassistant.ai.chat.ChatToken;
import com.jairam.aicodeassistant.conversation.domain.ChatSession;
import com.jairam.aicodeassistant.conversation.domain.ChatSessionStore;
import com.jairam.aicodeassistant.conversation.domain.ChatTurn;
import com.jairam.aicodeassistant.conversation.domain.Citation;
import com.jairam.aicodeassistant.platform.error.ResourceNotFoundException;
import com.jairam.aicodeassistant.retrieval.search.CodeSearch;
import com.jairam.aicodeassistant.retrieval.search.SearchQuery;
import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RAG orchestration for a chat turn: retrieve grounding chunks, assemble a guardrail-fenced prompt
 * with windowed memory, generate an answer, and persist the turn with citations.
 *
 * <p>Provides a blocking {@link #ask} and a streaming {@link #askStream}. In both the user turn is
 * recorded before generation and the assistant turn (with the citations derived from the retrieved
 * chunks) is recorded after — for streaming, the caller drives the token stream and then invokes
 * {@link StreamingAnswer#complete} so the final text is persisted once known.
 */
@Service
public class RagChatService {

  private static final Logger log = LoggerFactory.getLogger(RagChatService.class);

  private final ChatSessionStore sessions;
  private final CodeSearch codeSearch;
  private final ChatModel chatModel;
  private final ConversationProperties properties;
  private final MeterRegistry metrics;
  private final Clock clock;

  public RagChatService(
      ChatSessionStore sessions,
      CodeSearch codeSearch,
      ChatModel chatModel,
      ConversationProperties properties,
      MeterRegistry metrics,
      Clock clock) {
    this.sessions = sessions;
    this.codeSearch = codeSearch;
    this.chatModel = chatModel;
    this.properties = properties;
    this.metrics = metrics;
    this.clock = clock;
  }

  /** Blocking ask: returns the answer + citations and persists both turns. */
  @Transactional
  public Answer ask(UUID sessionId, String question) {
    Timer.Sample sample = Timer.start(metrics);
    ChatSession session = load(sessionId);
    PreparedTurn prepared = prepare(session, question);

    ChatResponse response = chatModel.chat(ChatRequest.of(prepared.prompt().messages()));
    String answer = response.content();

    session.addAssistantTurn(answer, prepared.prompt().citations(), clock.instant());
    sessions.save(session);
    sample.stop(
        Timer.builder("conversation.rag.answer")
            .description("End-to-end blocking RAG answer latency")
            .register(metrics));
    log.info(
        "Answered turn in session {} with {} citations",
        sessionId,
        prepared.prompt().citations().size());
    return new Answer(answer, prepared.prompt().citations());
  }

  /**
   * Streaming ask: records the user turn, returns the token stream + citations, and a completion
   * hook to persist the assistant turn once the full text is assembled by the caller.
   */
  @Transactional
  public StreamingAnswer askStream(UUID sessionId, String question) {
    ChatSession session = load(sessionId);
    PreparedTurn prepared = prepare(session, question);
    Stream<ChatToken> tokens = chatModel.chatStream(ChatRequest.of(prepared.prompt().messages()));
    return new StreamingAnswer(
        prepared.prompt().citations(),
        tokens,
        fullText -> persistAssistantTurn(sessionId, fullText, prepared.prompt().citations()));
  }

  /** Persists the assistant turn after a stream completes. Separate tx from the stream. */
  @Transactional
  public void persistAssistantTurn(UUID sessionId, String fullText, List<Citation> citations) {
    ChatSession session = load(sessionId);
    session.addAssistantTurn(fullText, citations, clock.instant());
    sessions.save(session);
  }

  private PreparedTurn prepare(ChatSession session, String question) {
    Instant now = clock.instant();
    session.addUserTurn(question, now);
    // Persist the user turn immediately so it is durable even if generation fails.
    List<ChatTurn> memoryBeforeThisTurn = session.recentTurns(properties.memoryWindowTurns() + 1);
    // Exclude the just-added user turn from "prior" memory (it's the current question).
    List<ChatTurn> priorTurns =
        memoryBeforeThisTurn.isEmpty()
            ? List.of()
            : memoryBeforeThisTurn.subList(0, memoryBeforeThisTurn.size() - 1);
    sessions.save(session);

    List<SearchResult> retrieved =
        codeSearch.search(
            new SearchQuery(
                session.organizationId(),
                session.repositoryId(),
                question,
                properties.retrievalLimit()));

    PromptAssembler assembler = new PromptAssembler(properties.contextCharBudget());
    return new PreparedTurn(assembler.assemble(question, retrieved, priorTurns));
  }

  private ChatSession load(UUID sessionId) {
    return sessions
        .findById(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));
  }

  private record PreparedTurn(PromptAssembler.AssembledPrompt prompt) {}

  /** Blocking answer with its citations. */
  public record Answer(String content, List<Citation> citations) {}

  /**
   * Streaming answer: the citations (known up front, from retrieval), the lazy token stream, and a
   * completion callback to persist the assistant turn.
   */
  public record StreamingAnswer(
      List<Citation> citations,
      Stream<ChatToken> tokens,
      java.util.function.Consumer<String> onComplete) {}
}
