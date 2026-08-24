package com.jairam.aicodeassistant.conversation.adapter.rest;

import com.jairam.aicodeassistant.ai.chat.ChatToken;
import com.jairam.aicodeassistant.conversation.adapter.rest.dto.AskRequest;
import com.jairam.aicodeassistant.conversation.adapter.rest.dto.CreateSessionRequest;
import com.jairam.aicodeassistant.conversation.adapter.rest.dto.RenameSessionRequest;
import com.jairam.aicodeassistant.conversation.adapter.rest.dto.SessionView;
import com.jairam.aicodeassistant.conversation.application.RagChatService;
import com.jairam.aicodeassistant.conversation.domain.ChatSession;
import com.jairam.aicodeassistant.conversation.domain.ChatSessionStore;
import com.jairam.aicodeassistant.conversation.domain.Citation;
import com.jairam.aicodeassistant.iam.api.OrganizationAccess;
import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import com.jairam.aicodeassistant.platform.error.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Repository Code Chat endpoints.
 *
 * <ul>
 *   <li>{@code POST /api/v1/chat/sessions} — start a session (org read access required).
 *   <li>{@code GET /api/v1/chat/sessions?organizationId=} — list the caller's sessions.
 *   <li>{@code GET /api/v1/chat/sessions/{id}} — a session with its turns.
 *   <li>{@code POST /api/v1/chat/sessions/{id}/messages} — ask a question; the answer streams back
 *       as Server-Sent Events: {@code token} events (deltas), then a {@code citations} event, then
 *       {@code done}. The assistant turn is persisted when the stream completes.
 * </ul>
 *
 * <p>Authorization: the caller must own the session and be able to read its organization (iam
 * {@code OrganizationAccess}).
 */
@RestController
@RequestMapping("/api/v1/chat")
class ChatController {

  private static final Logger log = LoggerFactory.getLogger(ChatController.class);
  private static final long SSE_TIMEOUT_MS = 120_000L;

  private final RagChatService ragChatService;
  private final ChatSessionStore sessions;
  private final OrganizationAccess organizationAccess;
  private final Clock clock;
  // Small pool to drive SSE streams off the request thread.
  private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

  ChatController(
      RagChatService ragChatService,
      ChatSessionStore sessions,
      OrganizationAccess organizationAccess,
      Clock clock) {
    this.ragChatService = ragChatService;
    this.sessions = sessions;
    this.organizationAccess = organizationAccess;
    this.clock = clock;
  }

  @PostMapping("/sessions")
  @ResponseStatus(HttpStatus.CREATED)
  SessionView createSession(
      Authentication authentication, @Valid @RequestBody CreateSessionRequest request) {
    UUID userId = currentUserId(authentication);
    requireOrgRead(userId, request.organizationId());
    String title =
        (request.title() == null || request.title().isBlank())
            ? "New conversation"
            : request.title();
    ChatSession session =
        ChatSession.start(
            request.organizationId(), request.repositoryId(), userId, title, clock.instant());
    return toView(sessions.save(session));
  }

  @GetMapping("/sessions")
  List<SessionView> listSessions(
      Authentication authentication, @RequestParam("organizationId") UUID organizationId) {
    UUID userId = currentUserId(authentication);
    requireOrgRead(userId, organizationId);
    return sessions.findByUserAndOrganization(userId, organizationId).stream()
        .map(ChatController::toSummaryView)
        .toList();
  }

  @GetMapping("/sessions/{id}")
  SessionView getSession(Authentication authentication, @PathVariable UUID id) {
    ChatSession session = requireOwnedSession(authentication, id);
    return toView(session);
  }

  @PatchMapping("/sessions/{id}")
  SessionView renameSession(
      Authentication authentication,
      @PathVariable UUID id,
      @Valid @RequestBody RenameSessionRequest request) {
    ChatSession session = requireOwnedSession(authentication, id);
    session.rename(request.title(), clock.instant());
    return toSummaryView(sessions.save(session));
  }

  @DeleteMapping("/sessions/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteSession(Authentication authentication, @PathVariable UUID id) {
    requireOwnedSession(authentication, id);
    sessions.deleteById(id);
  }

  @PostMapping(value = "/sessions/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter ask(
      Authentication authentication,
      @PathVariable UUID id,
      @Valid @RequestBody AskRequest request) {
    requireOwnedSession(authentication, id);

    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
    RagChatService.StreamingAnswer answer = ragChatService.askStream(id, request.question());

    streamExecutor.execute(
        () -> {
          StringBuilder full = new StringBuilder();
          try (Stream<ChatToken> tokens = answer.tokens()) {
            var it = tokens.iterator();
            while (it.hasNext()) {
              ChatToken token = it.next();
              if (!token.delta().isEmpty()) {
                full.append(token.delta());
                emitter.send(SseEmitter.event().name("token").data(token.delta()));
              }
            }
            // Persist the assistant turn now that the full text is known.
            answer.onComplete().accept(full.toString());
            emitter.send(
                SseEmitter.event().name("citations").data(citationViews(answer.citations())));
            emitter.send(SseEmitter.event().name("done").data(""));
            emitter.complete();
          } catch (Exception e) {
            log.warn("Chat stream failed for session {}: {}", id, e.getMessage());
            emitter.completeWithError(e);
          }
        });
    return emitter;
  }

  // --- helpers -----------------------------------------------------------------

  private ChatSession requireOwnedSession(Authentication authentication, UUID sessionId) {
    UUID userId = currentUserId(authentication);
    ChatSession session =
        sessions
            .findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));
    if (!session.userId().equals(userId)) {
      // Do not reveal existence of another user's session.
      throw new ResourceNotFoundException("ChatSession", sessionId);
    }
    requireOrgRead(userId, session.organizationId());
    return session;
  }

  private void requireOrgRead(UUID userId, UUID organizationId) {
    if (!organizationAccess.canRead(userId, organizationId)) {
      throw new NotAuthorizedException(organizationId);
    }
  }

  private static List<SessionView.CitationView> citationViews(List<Citation> citations) {
    return citations.stream()
        .map(
            c ->
                new SessionView.CitationView(
                    c.index(),
                    c.chunkId(),
                    c.repositoryId(),
                    c.filePath(),
                    c.startLine(),
                    c.endLine()))
        .toList();
  }

  private static SessionView toView(ChatSession s) {
    List<SessionView.TurnView> turns =
        s.turns().stream()
            .map(
                t ->
                    new SessionView.TurnView(
                        t.seq(), t.role().name(), t.content(), citationViews(t.citations())))
            .toList();
    return new SessionView(
        s.id(),
        s.organizationId(),
        s.repositoryId(),
        s.title(),
        s.createdAt(),
        s.updatedAt(),
        turns);
  }

  private static SessionView toSummaryView(ChatSession s) {
    return new SessionView(
        s.id(),
        s.organizationId(),
        s.repositoryId(),
        s.title(),
        s.createdAt(),
        s.updatedAt(),
        List.of());
  }

  private static UUID currentUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotAuthenticatedException();
    }
    return UUID.fromString(authentication.getName());
  }

  /** Caller cannot read the organization. HTTP 403. */
  static final class NotAuthorizedException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    NotAuthorizedException(UUID organizationId) {
      super(
          ErrorType.AUTHORIZATION,
          HttpStatus.FORBIDDEN,
          "You cannot access this organization",
          Map.of("organizationId", organizationId.toString()));
    }
  }

  /** No authenticated principal. HTTP 401. */
  static final class NotAuthenticatedException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    NotAuthenticatedException() {
      super(ErrorType.AUTHENTICATION, HttpStatus.UNAUTHORIZED, "Not authenticated", Map.of());
    }
  }
}
