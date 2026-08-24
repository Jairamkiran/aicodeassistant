package com.jairam.aicodeassistant.audit.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link AuditEventEntity} (insert-only in practice). */
interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, UUID> {

  List<AuditEventEntity> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
