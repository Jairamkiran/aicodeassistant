-- =============================================================================
-- V9 — Full-text search over code chunks (Milestone 5).
--
-- Adds lexical search capability to the code_chunks table (written by M4) using
-- Postgres' built-in full-text search — NO new infrastructure and NO dual-write
-- (chunks already live here). Hybrid search fuses this with pgvector cosine KNN
-- via reciprocal-rank fusion. See ADR-0011 for why Postgres FTS over OpenSearch.
--
-- The tsvector is a STORED GENERATED column, so it is maintained automatically
-- by Postgres on insert/update — no application code or backfill required.
-- =============================================================================

ALTER TABLE code_chunks
    ADD COLUMN content_fts tsvector
        GENERATED ALWAYS AS (to_tsvector('english', content)) STORED;

-- GIN index for fast @@ tsquery matching.
CREATE INDEX idx_code_chunks_fts ON code_chunks USING gin (content_fts);
