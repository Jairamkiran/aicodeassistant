-- =============================================================================
-- V2 — Identity & Access (Milestone 1).
--
-- Users, organizations, memberships (RBAC), and refresh-token families for
-- rotation + reuse detection. All ids are UUIDs (server-generated via
-- gen_random_uuid from the pgcrypto extension enabled in V1).
-- =============================================================================

-- --- Users -------------------------------------------------------------------
CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          TEXT        NOT NULL,
    password_hash  TEXT        NOT NULL,
    display_name   TEXT        NOT NULL,
    status         TEXT        NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version        BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

-- Case-insensitive uniqueness on email (we also normalise to lower-case in the
-- domain, but enforce it here as the last line of defence).
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

-- --- Organizations -----------------------------------------------------------
CREATE TABLE organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL,
    slug        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

-- --- Memberships (user <-> organization, with a role) ------------------------
CREATE TABLE memberships (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL,
    organization_id  UUID        NOT NULL,
    role             TEXT        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_memberships_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_memberships_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT uq_memberships_user_org UNIQUE (user_id, organization_id),
    CONSTRAINT ck_memberships_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

CREATE INDEX idx_memberships_org ON memberships (organization_id);
CREATE INDEX idx_memberships_user ON memberships (user_id);

-- --- Refresh tokens (family-based rotation + reuse detection) ----------------
-- Each login starts a token "family". Rotation issues a new token in the same
-- family and marks the previous one used. If a token that is already used (or
-- revoked) is presented again, the entire family is revoked — the signal of a
-- stolen token being replayed.
CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL,
    family_id    UUID        NOT NULL,
    token_hash   TEXT        NOT NULL,
    issued_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL,
    used_at      TIMESTAMPTZ,
    revoked_at   TIMESTAMPTZ,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
-- Cheap lookup of live tokens for cleanup / revocation scans.
CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens (expires_at)
    WHERE used_at IS NULL AND revoked_at IS NULL;
