-- =============================================================================
-- V12 — Repository cascade foreign keys (Milestone 11, repository lifecycle).
--
-- Deleting a repository must also remove its indexed chunks and its index-job
-- row; otherwise a delete leaves orphaned chunks that still surface in search.
-- The original V7/V8 tables carried only a repository_id column (no FK, so the
-- write side stayed decoupled from the repositories table's existence at insert
-- time). Now that repositories can be deleted (M11), add ON DELETE CASCADE FKs
-- so the database enforces cleanup atomically.
-- =============================================================================

ALTER TABLE code_chunks
    ADD CONSTRAINT fk_code_chunks_repository
        FOREIGN KEY (repository_id) REFERENCES repositories (id) ON DELETE CASCADE;

ALTER TABLE index_jobs
    ADD CONSTRAINT fk_index_jobs_repository
        FOREIGN KEY (repository_id) REFERENCES repositories (id) ON DELETE CASCADE;
