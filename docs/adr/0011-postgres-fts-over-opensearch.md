# 0011. Postgres full-text search over OpenSearch for lexical retrieval

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

M5 adds lexical (keyword) search to complement pgvector semantic search, fused
via reciprocal-rank fusion. The roadmap named Elasticsearch/OpenSearch as the
lexical engine. The chunks being searched already live in Postgres
(`code_chunks`, written by the M4 saga).

Adopting OpenSearch now would mean:
- a **new hard infrastructure dependency** (a search cluster to run, secure, and operate), and
- a **dual write** — every chunk indexed into both pgvector and OpenSearch, with
  the consistency and failure-handling that a second write target requires (the
  M4 upsert step would need to write two stores atomically-ish).

## Decision

Use **Postgres full-text search** for lexical retrieval: a generated
`tsvector` column on `code_chunks` (`content_fts`) with a GIN index, queried via
`plainto_tsquery` + `ts_rank`. Hybrid = pgvector cosine KNN + Postgres FTS, fused
with RRF, all in one store and one query path.

OpenSearch is **deferred** until relevance quality or scale genuinely demands a
dedicated search engine (e.g. advanced analyzers, very large corpora, faceting).

## Consequences

- **Positive:** zero new infrastructure; **no dual write** (the generated column
  is maintained by Postgres automatically — no app code, no backfill); one store
  to reason about, back up, and test (Testcontainers covers it end-to-end);
  transactional consistency with the vector data for free.
- **Negative:** Postgres FTS is less feature-rich than a dedicated engine
  (simpler analyzers/ranking, English config here, no BM25 tuning knobs). Adequate
  for code search at current scale; if metrics later show it limiting, OpenSearch
  becomes a justified addition — the `CodeSearch` port + `LexicalSearchDao` seam
  makes swapping the lexical backend localized.

## Alternatives considered

- **OpenSearch now (as roadmapped).** Rejected: premature infrastructure + a
  dual-write for no demonstrated relevance/scale need — the exact "don't add infra
  without a consumer" smell the review directive rejects.
- **Vector-only.** Rejected: loses exact identifier / error-string matching that
  code search specifically needs, and drops the hybrid-fusion story.
