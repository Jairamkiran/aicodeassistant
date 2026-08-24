package com.jairam.aicodeassistant.audit.domain;

/**
 * Outbound port for appending audit events. Implemented by a persistence adapter. Deliberately
 * append-only: there is no update or delete.
 */
public interface AuditEventStore {

  /** Appends one event to the audit log. */
  void append(AuditEvent event);
}
