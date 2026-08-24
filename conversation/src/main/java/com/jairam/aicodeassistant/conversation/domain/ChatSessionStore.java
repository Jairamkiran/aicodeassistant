package com.jairam.aicodeassistant.conversation.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for persisting chat sessions and their turns. */
public interface ChatSessionStore {

  ChatSession save(ChatSession session);

  Optional<ChatSession> findById(UUID sessionId);

  /** Deletes a session (and its turns, via cascade) by id. No-op if absent. */
  void deleteById(UUID sessionId);

  /** Sessions owned by a user in an organization, most recently updated first. */
  List<ChatSession> findByUserAndOrganization(UUID userId, UUID organizationId);
}
