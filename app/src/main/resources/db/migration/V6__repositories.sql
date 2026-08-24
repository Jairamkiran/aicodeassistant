-- =============================================================================
-- V6 — Repositories (Milestone 3).
--
-- A repository registered into an organization for indexing. In M3 an import
-- captures metadata and emits RepositoryImportRequested; the actual
-- clone/parse/embed pipeline (moving the row REGISTERED -> IMPORTING -> READY)
-- is the indexing worker + saga in M4.
-- =============================================================================

CREATE TABLE repositories (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID        NOT NULL,
    provider         TEXT        NOT NULL DEFAULT 'GITHUB',
    external_id      TEXT        NOT NULL,
    owner            TEXT        NOT NULL,
    name             TEXT        NOT NULL,
    clone_url        TEXT        NOT NULL,
    default_branch   TEXT        NOT NULL DEFAULT 'main',
    is_private       BOOLEAN     NOT NULL DEFAULT FALSE,
    status           TEXT        NOT NULL DEFAULT 'REGISTERED',
    registered_by    UUID        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    status_detail    TEXT,
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_repositories_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT ck_repositories_provider CHECK (provider IN ('GITHUB')),
    CONSTRAINT ck_repositories_status
        CHECK (status IN ('REGISTERED', 'IMPORTING', 'READY', 'FAILED')),
    -- The same external repo can only be registered once per organization.
    CONSTRAINT uq_repositories_org_provider_external
        UNIQUE (organization_id, provider, external_id)
);

CREATE INDEX idx_repositories_org ON repositories (organization_id);
CREATE INDEX idx_repositories_status ON repositories (status);
