package com.jairam.aicodeassistant.audit.adapter.persistence;

import com.jairam.aicodeassistant.audit.AuditQuery;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** JPA-backed read side of the audit log. */
@Component
class JpaAuditQuery implements AuditQuery {

  private final AuditEventJpaRepository jpa;

  JpaAuditQuery(AuditEventJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return jpa.count();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Entry> recent(int limit) {
    return jpa.findAllByOrderByOccurredAtDesc(PageRequest.of(0, Math.max(1, limit))).stream()
        .map(e -> new Entry(e.getEventType(), e.getOutcome(), e.getActorType(), e.getActorId()))
        .toList();
  }
}
