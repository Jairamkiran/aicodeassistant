package com.jairam.aicodeassistant.conversation.adapter.persistence;

import com.jairam.aicodeassistant.conversation.domain.ChatSession;
import com.jairam.aicodeassistant.conversation.domain.ChatSessionStore;
import com.jairam.aicodeassistant.conversation.domain.ChatTurn;
import com.jairam.aicodeassistant.conversation.domain.Citation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** JPA-backed {@link ChatSessionStore}. Maps the aggregate (session + turns) to/from entities. */
@Component
class JpaChatSessionStore implements ChatSessionStore {

  private final ChatSessionJpaRepository jpa;

  JpaChatSessionStore(ChatSessionJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  @Transactional
  public ChatSession save(ChatSession session) {
    ChatSessionEntity entity =
        jpa.findById(session.id())
            .orElseGet(
                () ->
                    new ChatSessionEntity(
                        session.id(),
                        session.organizationId(),
                        session.repositoryId(),
                        session.userId(),
                        session.title(),
                        session.createdAt(),
                        session.updatedAt(),
                        session.version()));
    entity.setTitle(session.title());
    entity.setUpdatedAt(session.updatedAt());
    entity.replaceTurns(session.turns().stream().map(JpaChatSessionStore::toTurnEntity).toList());
    return toDomain(jpa.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ChatSession> findById(UUID sessionId) {
    return jpa.findById(sessionId).map(JpaChatSessionStore::toDomain);
  }

  @Override
  @Transactional
  public void deleteById(UUID sessionId) {
    jpa.deleteById(sessionId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChatSession> findByUserAndOrganization(UUID userId, UUID organizationId) {
    return jpa.findByUserIdAndOrganizationIdOrderByUpdatedAtDesc(userId, organizationId).stream()
        .map(JpaChatSessionStore::toDomain)
        .toList();
  }

  private static ChatTurnEntity toTurnEntity(ChatTurn turn) {
    List<ChatTurnEntity.CitationRow> rows =
        turn.citations().stream()
            .map(
                c ->
                    new ChatTurnEntity.CitationRow(
                        c.index(),
                        c.chunkId(),
                        c.repositoryId(),
                        c.filePath(),
                        c.startLine(),
                        c.endLine()))
            .toList();
    return new ChatTurnEntity(
        UUID.randomUUID(), turn.seq(), turn.role().name(), turn.content(), rows, turn.createdAt());
  }

  private static ChatSession toDomain(ChatSessionEntity e) {
    List<ChatTurn> turns =
        e.getTurns().stream()
            .map(
                t ->
                    new ChatTurn(
                        t.getSeq(),
                        ChatTurn.Role.valueOf(t.getRole()),
                        t.getContent(),
                        t.getCitations().stream()
                            .map(
                                r ->
                                    new Citation(
                                        r.index(),
                                        r.chunkId(),
                                        r.repositoryId(),
                                        r.filePath(),
                                        r.startLine(),
                                        r.endLine()))
                            .toList(),
                        t.getCreatedAt()))
            .toList();
    return ChatSession.rehydrate(
        e.getId(),
        e.getOrganizationId(),
        e.getRepositoryId(),
        e.getUserId(),
        e.getTitle(),
        turns,
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getVersion());
  }
}
