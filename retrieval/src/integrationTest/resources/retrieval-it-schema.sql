-- Minimal schema for the retrieval integration test: the code_chunks table with
-- pgvector embedding + FTS generated column. Mirrors app migrations V1/V8/V9 for
-- the columns this module needs. (The app owns the authoritative migrations; this
-- self-contained script lets the retrieval IT stand alone against a container.)
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS code_chunks (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id    UUID         NOT NULL,
    organization_id  UUID         NOT NULL,
    file_path        TEXT         NOT NULL,
    language         TEXT,
    start_line       INT          NOT NULL,
    end_line         INT          NOT NULL,
    content          TEXT         NOT NULL,
    embedding        vector(768)  NOT NULL,
    content_fts      tsvector     GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_code_chunks_repository ON code_chunks (repository_id);
CREATE INDEX IF NOT EXISTS idx_code_chunks_embedding ON code_chunks USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_code_chunks_fts ON code_chunks USING gin (content_fts);
