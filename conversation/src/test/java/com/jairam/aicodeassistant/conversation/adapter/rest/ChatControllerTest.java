package com.jairam.aicodeassistant.conversation.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jairam.aicodeassistant.ai.chat.ChatToken;
import com.jairam.aicodeassistant.ai.chat.TokenUsage;
import com.jairam.aicodeassistant.conversation.adapter.rest.dto.AskRequest;
import com.jairam.aicodeassistant.conversation.adapter.rest.dto.CreateSessionRequest;
import com.jairam.aicodeassistant.conversation.application.RagChatService;
import com.jairam.aicodeassistant.conversation.domain.ChatSession;
import com.jairam.aicodeassistant.conversation.domain.ChatSessionStore;
import com.jairam.aicodeassistant.conversation.domain.Citation;
import com.jairam.aicodeassistant.iam.api.OrganizationAccess;
import com.jairam.aicodeassistant.platform.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Unit tests for the chat controller: session-creation authorization, session ownership
 * enforcement, and the SSE ask path producing a completing emitter. Collaborators are mocked/faked
 * — no web server, no DB, no LLM (kept simple per the review directive; full HTTP is covered by
 * other layers).
 */
class ChatControllerTest {

  private static final UUID USER = UUID.randomUUID();
  private static final UUID OTHER_USER = UUID.randomUUID();
  private static final UUID ORG = UUID.randomUUID();
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

  private final ChatSessionStore sessions = mock(ChatSessionStore.class);
  private final RagChatService rag = mock(RagChatService.class);
  private final OrganizationAccess access = mock(OrganizationAccess.class);
  private final ChatController controller = new ChatController(rag, sessions, access, clock);

  private static Authentication auth(UUID userId) {
    return new UsernamePasswordAuthenticationToken(userId.toString(), "n/a", List.of());
  }

  @Test
  void createSessionRequiresOrgReadAccess() {
    when(access.canRead(USER, ORG)).thenReturn(false);
    assertThatThrownBy(
            () -> controller.createSession(auth(USER), new CreateSessionRequest(ORG, null, "t")))
        .isInstanceOf(ChatController.NotAuthorizedException.class);
  }

  @Test
  void createSessionPersistsWhenAuthorized() {
    when(access.canRead(USER, ORG)).thenReturn(true);
    when(sessions.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

    var view = controller.createSession(auth(USER), new CreateSessionRequest(ORG, null, "My chat"));

    assertThat(view.title()).isEqualTo("My chat");
    assertThat(view.organizationId()).isEqualTo(ORG);
  }

  @Test
  void accessingAnotherUsersSessionIsNotFound() {
    ChatSession othersSession = ChatSession.start(ORG, null, OTHER_USER, "s", clock.instant());
    when(sessions.findById(othersSession.id())).thenReturn(Optional.of(othersSession));

    assertThatThrownBy(() -> controller.getSession(auth(USER), othersSession.id()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void askStreamsTokensThenCompletes() throws Exception {
    ChatSession session = ChatSession.start(ORG, null, USER, "s", clock.instant());
    when(sessions.findById(session.id())).thenReturn(Optional.of(session));
    when(access.canRead(USER, ORG)).thenReturn(true);

    boolean[] persisted = {false};
    when(rag.askStream(session.id(), "hi"))
        .thenReturn(
            new RagChatService.StreamingAnswer(
                List.of(new Citation(1, UUID.randomUUID(), UUID.randomUUID(), "A.java", 1, 5)),
                java.util.stream.Stream.of(
                    ChatToken.delta("Hel"),
                    ChatToken.delta("lo"),
                    ChatToken.done(TokenUsage.UNKNOWN)),
                text -> persisted[0] = true));

    SseEmitter emitter = controller.ask(auth(USER), session.id(), new AskRequest("hi"));

    assertThat(emitter).isNotNull();
    // The background stream thread should complete and invoke the persist callback.
    await()
        .atMost(java.time.Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(persisted[0]).isTrue());
  }
}
