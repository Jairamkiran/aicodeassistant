package com.jairam.aicodeassistant.audit.application;

import com.jairam.aicodeassistant.audit.AuditRecorder;
import com.jairam.aicodeassistant.audit.domain.AuditEvent;
import com.jairam.aicodeassistant.audit.domain.AuditEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link AuditRecorder}: appends events via the {@link AuditEventStore}.
 *
 * <p>Two deliberate reliability choices:
 *
 * <ul>
 *   <li><b>Never throws.</b> Auditing must not break the business operation it observes; a storage
 *       failure is logged (at ERROR — a failed audit write is itself security-relevant) and
 *       swallowed.
 *   <li><b>{@code REQUIRES_NEW}.</b> The audit row commits independently of the caller's
 *       transaction, so an event recorded for a <em>failed</em> operation (e.g. a failed login that
 *       rolls back) is still persisted.
 * </ul>
 */
@Service
public class AuditService implements AuditRecorder {

  private static final Logger log = LoggerFactory.getLogger(AuditService.class);

  private final AuditEventStore store;

  public AuditService(AuditEventStore store) {
    this.store = store;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(AuditEvent event) {
    try {
      store.append(event);
    } catch (RuntimeException e) {
      log.error(
          "Failed to write audit event type={} outcome={} actor={}",
          event.eventType(),
          event.outcome(),
          event.actorId(),
          e);
    }
  }
}
