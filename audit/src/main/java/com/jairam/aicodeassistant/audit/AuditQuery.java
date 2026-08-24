package com.jairam.aicodeassistant.audit;

import java.util.List;

/**
 * Public read API of the audit context: query recorded events.
 *
 * <p>Minimal by design for M2 — enough for verification and a future admin view, without
 * speculating on filters/pagination that no caller needs yet.
 */
public interface AuditQuery {

  /** Total number of recorded audit events. */
  long count();

  /** The most recent events (up to {@code limit}), newest first. */
  List<Entry> recent(int limit);

  /** A read-only projection of an audit event. */
  record Entry(String eventType, String outcome, String actorType, String actorId) {}
}
