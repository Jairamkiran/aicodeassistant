-- =============================================================================
-- V5 — GitHub connections (Milestone 3).
--
-- One row per user who has linked their GitHub account via OAuth. The access
-- token is stored ENCRYPTED (AES-256-GCM ciphertext string, never plaintext);
-- see the EncryptionService in the platform kernel. Owned by the integration
-- module — no other context reads this table or the token.
-- =============================================================================

CREATE TABLE github_connections (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID        NOT NULL,
    github_login       TEXT        NOT NULL,
    github_user_id     BIGINT      NOT NULL,
    access_token_enc   TEXT        NOT NULL,
    scopes             TEXT        NOT NULL DEFAULT '',
    connected_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    version            BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_github_connections_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- One GitHub connection per user (re-linking updates the existing row).
    CONSTRAINT uq_github_connections_user UNIQUE (user_id)
);

CREATE INDEX idx_github_connections_login ON github_connections (github_login);
