-- =============================================================================
-- V7 — Index jobs (Milestone 4).
--
-- Owned by the `indexing` bounded context. One job per repository import,
-- carrying the saga's state. The worker CLAIMS a job with an atomic conditional
-- update (status REGISTERED -> CLAIMED) so exactly one worker processes it —
-- this replaces a distributed lock (see ADR-0009), avoiding the lock-TTL vs
-- multi-minute-clone double-processing hazard.
-- =============================================================================

CREATE TABLE index_jobs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id     UUID        NOT NULL,
    organization_id   UUID        NOT NULL,
    clone_url         TEXT        NOT NULL,
    default_branch    TEXT        NOT NULL DEFAULT 'main',
    status            TEXT        NOT NULL DEFAULT 'REGISTERED',
    attempts          INT         NOT NULL DEFAULT 0,
    status_detail     TEXT,
    chunk_count       INT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT      NOT NULL DEFAULT 0,
    -- One job per repository; a re-import reuses/resets the row.
    CONSTRAINT uq_index_jobs_repository UNIQUE (repository_id),
    CONSTRAINT ck_index_jobs_status CHECK (status IN (
        'REGISTERED', 'CLAIMED', 'CLONING', 'PARSING', 'EMBEDDING',
        'UPSERTING', 'INDEXED', 'FAILED'))
);

CREATE INDEX idx_index_jobs_status ON index_jobs (status);
