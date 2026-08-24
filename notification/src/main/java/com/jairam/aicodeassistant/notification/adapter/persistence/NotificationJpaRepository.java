package com.jairam.aicodeassistant.notification.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link NotificationEntity}. */
interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

  List<NotificationEntity> findByRecipientUserIdOrderByCreatedAtDesc(
      UUID recipientUserId, Pageable pageable);

  long countByRecipientUserIdAndReadFalse(UUID recipientUserId);
}
