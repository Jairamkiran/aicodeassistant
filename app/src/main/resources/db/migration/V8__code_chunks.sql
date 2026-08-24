-- =============================================================================
-- V8 — Code chunks + embeddings (Milestone 4).
--
-- Owned by the `retrieval` bounded context. Each row is a chunk of a repository
-- file plus its embedding vector. M4 writes these (the indexing saga's upsert
-- step); M5 adds the query/search side. Uses the pgvector `vector` type enabled
-- by V1.
--
-- Dimension 768 matches the default embedding model (nomic-embed-text). If the
-- model/dimension changes, a new migration recreates the column + index.
-- =============================================================================

CREATE TABLE code_chunks (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id    UUID         NOT NULL,
    organization_id  UUID         NOT NULL,
    file_path        TEXT         NOT NULL,
    language         TEXT,
    start_line       INT          NOT NULL,
    end_line         INT          NOT NULL,
    content          TEXT         NOT NULL,
    embedding        vector(768)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_code_chunks_repository ON code_chunks (repository_id);

-- Approximate-nearest-neighbour index for cosine similarity search (used in M5).
-- HNSW gives good recall/latency; built now so the write path populates it.
CREATE INDEX idx_code_chunks_embedding
    ON code_chunks USING hnsw (embedding vector_cosine_ops);
