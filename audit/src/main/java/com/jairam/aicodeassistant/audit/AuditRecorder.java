package com.jairam.aicodeassistant.audit;

import com.jairam.aicodeassistant.audit.domain.AuditEvent;

/**
 * Public API of the audit context: record an audit event.
 *
 * <p>This is the only type other modules use; everything else in the context is internal. Kept as
 * an interface so callers depend on a stable contract, and so a no-op implementation can be
 * substituted in tests that do not care about auditing.
 */
public interface AuditRecorder {

  /** Records an audit event (append-only). Must not throw on storage failure. */
  void record(AuditEvent event);
}
