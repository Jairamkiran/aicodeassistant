-- =============================================================================
-- V1 — Baseline schema.
--
-- Establishes the extensions and infrastructure tables that later milestones
-- build upon. Business tables (users, repositories, ...) are added by the
-- migrations that accompany their owning milestone (V2+, per bounded context).
--
-- This migration is intentionally infrastructure-only so M0 has a real, tested
-- Flyway baseline without pre-committing schema that later features will design.
-- =============================================================================

-- pgvector: required by the retrieval context (M4/M5) for embedding storage.
-- Declared here so the extension exists before any vector column is created.
CREATE EXTENSION IF NOT EXISTS vector;

-- pgcrypto: gen_random_uuid() for server-side UUID defaults used across contexts.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------------------------------------------------------
-- Transactional outbox (Spring Modulith event publication registry).
--
-- Modulith persists externalized domain events here in the SAME transaction as
-- the state change, then relays them to Kafka. This is what guarantees
-- at-least-once delivery without a dual write. Modulith's JPA schema
-- initializer can create this, but we own it explicitly so the schema is
-- versioned and reviewable rather than created implicitly at runtime.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_publication (
    id               UUID         NOT NULL,
    listener_id      TEXT         NOT NULL,
    event_type       TEXT         NOT NULL,
    serialized_event TEXT         NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

-- Incomplete publications (completion_date IS NULL) are the ones needing relay;
-- this partial index keeps the resubmission scan cheap as the table grows.
CREATE INDEX IF NOT EXISTS idx_event_publication_incomplete
    ON event_publication (publication_date)
    WHERE completion_date IS NULL;

CREATE INDEX IF NOT EXISTS idx_event_publication_completion
    ON event_publication (completion_date);
