-- =============================================================================
-- V11 — Notifications (Milestone 11, Platform Improvements).
--
-- Per-user in-app notifications created from neutral NotificationSignals (e.g.
-- a repository finishing or failing indexing). Read-mostly, listed newest-first
-- per recipient with an unread count; no cross-row relationships beyond the
-- owning user, so a single flat table is the right shape.
-- =============================================================================

CREATE TABLE notifications (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID        NOT NULL,
    organization_id   UUID        NOT NULL,
    type              TEXT        NOT NULL,
    title             TEXT        NOT NULL,
    message           TEXT,
    resource_type     TEXT,
    resource_id       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_read           BOOLEAN     NOT NULL DEFAULT FALSE
);

-- Primary access path: a recipient's notifications, newest first.
CREATE INDEX idx_notifications_recipient_created
    ON notifications (recipient_user_id, created_at DESC);
