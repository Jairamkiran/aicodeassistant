-- =============================================================================
-- V10 — Conversations (Milestone 7, flagship Repository Code Chat).
--
-- A chat session is a conversation scoped to an organization (and optionally a
-- single repository) owned by a user. Each turn is one message; assistant turns
-- carry citations to the retrieved code chunks that grounded the answer.
--
-- Citations are stored as a JSON TEXT column on the turn: a small, read-mostly
-- list derived from retrieval, always loaded with the turn and never queried
-- independently — a child table would be over-normalisation, and TEXT (vs JSONB)
-- keeps the mapping identical across Postgres and the H2 test database.
-- =============================================================================

CREATE TABLE chat_sessions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID        NOT NULL,
    repository_id    UUID,
    user_id          UUID        NOT NULL,
    title            TEXT        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_chat_sessions_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_sessions_user ON chat_sessions (user_id);
CREATE INDEX idx_chat_sessions_org ON chat_sessions (organization_id);

CREATE TABLE chat_turns (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id   UUID        NOT NULL,
    seq          INT         NOT NULL,
    role         TEXT        NOT NULL,
    content      TEXT        NOT NULL,
    citations    TEXT        NOT NULL DEFAULT '[]',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_chat_turns_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions (id) ON DELETE CASCADE,
    CONSTRAINT uq_chat_turns_session_seq UNIQUE (session_id, seq),
    CONSTRAINT ck_chat_turns_role CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX idx_chat_turns_session ON chat_turns (session_id);
