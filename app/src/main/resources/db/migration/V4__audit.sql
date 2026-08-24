-- =============================================================================
-- V4 — Audit log (Milestone 2).
--
-- Append-only record of security-relevant events. Rows are only ever INSERTed;
-- the application never issues UPDATE/DELETE. There is no version column and no
-- natural mutable state — immutability is the point.
-- =============================================================================

CREATE TABLE audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    event_type      TEXT        NOT NULL,
    outcome         TEXT        NOT NULL,
    actor_type      TEXT        NOT NULL,
    actor_id        TEXT,
    target_type     TEXT,
    target_id       TEXT,
    correlation_id  TEXT,
    client_ip       TEXT,
    detail          TEXT,
    CONSTRAINT ck_audit_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE'))
);

-- Common query axes: by time (recent activity), by actor, and by event type.
CREATE INDEX idx_audit_events_occurred_at ON audit_events (occurred_at DESC);
CREATE INDEX idx_audit_events_actor ON audit_events (actor_type, actor_id);
CREATE INDEX idx_audit_events_type ON audit_events (event_type);
