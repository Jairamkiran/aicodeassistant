-- =============================================================================
-- V3 — API keys (Milestone 2).
--
-- Programmatic access credentials owned by a user. The raw key is shown once at
-- creation; only a hash of the secret is stored. Keys carry scopes, an optional
-- expiry, and a status, and record last-use for observability.
--
-- Key format presented to clients:  aca_<prefix>.<secret>
--   - prefix : short, non-secret public identifier (indexed lookup)
--   - secret : high-entropy random value; only its hash is persisted
-- =============================================================================

CREATE TABLE api_keys (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL,
    name          TEXT        NOT NULL,
    key_prefix    TEXT        NOT NULL,
    secret_hash   TEXT        NOT NULL,
    scopes        TEXT        NOT NULL DEFAULT '',
    status        TEXT        NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ,
    last_used_at  TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ,
    CONSTRAINT fk_api_keys_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_api_keys_prefix UNIQUE (key_prefix),
    CONSTRAINT ck_api_keys_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE INDEX idx_api_keys_user ON api_keys (user_id);
-- Authentication looks keys up by their non-secret prefix, then verifies the
-- secret hash — so this index is the hot path for API-key auth.
CREATE UNIQUE INDEX uq_api_keys_prefix_active
    ON api_keys (key_prefix)
    WHERE status = 'ACTIVE';
