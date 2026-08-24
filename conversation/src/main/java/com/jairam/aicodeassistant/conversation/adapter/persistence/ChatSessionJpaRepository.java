package com.jairam.aicodeassistant.conversation.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ChatSessionEntity}. */
interface ChatSessionJpaRepository extends JpaRepository<ChatSessionEntity, UUID> {

  List<ChatSessionEntity> findByUserIdAndOrganizationIdOrderByUpdatedAtDesc(
      UUID userId, UUID organizationId);
}
