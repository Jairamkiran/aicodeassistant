# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project has not yet cut a numbered release; entries are grouped by
development milestone. Dates are the milestone completion dates.

## [Unreleased]

_The product is feature-complete across M0–M13: a full-stack, production-ready
AI software-engineering assistant with a React web client, structured AI code
review, caching/observability, in-app notifications, repository/session
lifecycle, and container/Kubernetes deployment._

## M13 — Finalization — 2026-07

### Changed
- Documentation overhaul: rewritten `README`, new `docs/DEPLOYMENT.md`,
  `docs/CONFIGURATION.md`, `docs/DEVELOPER_GUIDE.md`, `docs/API.md`; refreshed
  `docs/ARCHITECTURE.md` + system diagram; ADRs 0015–0018 added.
- Dependency cleanup: removed unused version-catalog entries (`redisson` —
  superseded by the atomic status-claim, ADR-0009; `spring-boot-starter-webflux`
  — the stack is servlet MVC + JDK HttpClient; `mapstruct` — all mapping is
  hand-written).

## M12 — Production Readiness — 2026-07

### Added
- **PMD** static analysis in the build convention (focused, low-false-positive
  ruleset; main sources only) — part of `./gradlew check`.
- **Frontend container**: multi-stage Node build → nginx serving the SPA with
  history fallback + SSE-friendly `/api` reverse proxy; non-root.
- **Kubernetes manifests** (`k8s/`): namespace, config map, secret template,
  `app`/`indexer-worker`/`frontend` Deployments + Services (non-root, dropped
  capabilities, read-only rootfs, liveness/readiness/startup probes), HPAs, and
  an SSE-friendly TLS ingress.
- **Security headers** filter in `platform-web` (nosniff, DENY, Referrer-Policy,
  no-store) on every response.
- **`application-prod.yml`**: actuator surface locked to health/info/prometheus,
  health details hidden, Secure refresh cookie, graceful shutdown, forwarded
  headers, pool/thread tuning, reduced trace sampling.
- **CI**: a frontend job (install/lint/test/build) + frontend image build.

## M11 — Platform Improvements — 2026-07

### Added
- **`notification` context**: in-app notifications from a neutral
  `NotificationSignal` in the shared kernel (mirrors `AuditSignal`, ADR-0018);
  persisted (`V11`), REST list/unread-count/mark-read, log-based dispatcher port
  (SMTP-replaceable). Repository indexing outcomes notify the importing user.
- **Repository lifecycle**: re-index and delete endpoints; `V12` adds
  `ON DELETE CASCADE` FKs so deleting a repository removes its chunks and index
  job (fixes a latent orphaned-chunk bug).
- **Chat sessions**: rename (PATCH) and delete (DELETE) endpoints.
- **Search**: optional language filter end-to-end.
- **Background workers**: scheduled refresh-token cleanup and a stalled
  index-job reaper (the ADR-0009 follow-up); scheduling enabled on app + worker.
- Grafana panels for search/RAG latency and LLM token throughput.

### Changed
- Frontend reconciled to the real backend contracts; a notification bell with an
  unread badge added to the app shell.

## M10 — Performance — 2026-07

### Added
- In-process **Caffeine cache** for the hot org-membership authorization read
  (ADR-0017), with immediate eviction on membership change.
- Micrometer latency timers for hybrid search and end-to-end RAG answers.
- Frontend: chat error recovery (retry a failed stream), skip-to-content link,
  and focusable main landmark for keyboard/AT users.

## M9 — AI Improvements — 2026-07

### Added
- **Provider-agnostic structured output** in `ai :: chat` (ADR-0015):
  `StructuredOutputs` (JSON instruction + tolerant balanced-JSON parser) that
  works on both Ollama and OpenAI with no vendor JSON-mode and no DTO leakage.
- **`codeintel` context**: AI **Code Review** producing structured findings
  (severity/file/line) — retrieve → JSON review → domain findings, org-authorized
  via `iam :: api`; `POST /api/v1/code-reviews`.
- **Retrieval ranking**: tunable weighted reciprocal-rank fusion
  (`aicodeassistant.retrieval`) + a deterministic ranking-evaluation harness
  (recall@k + MRR).
- Frontend: a Code Review page with severity-grouped findings.

## M8 — Web UI — 2026-07

### Added
- **React + TypeScript + Vite + MUI + TanStack Query + Monaco** SPA (ADR-0016):
  login/register with in-memory access token + silent refresh, org switcher,
  repository dashboard + GitHub import + live indexing status, hybrid search,
  the flagship **SSE-streaming chat** with clickable citations that open the
  cited span in Monaco, session history, settings (profile / dark mode / API
  keys). Responsive layout, light/dark theme, route-level code splitting.
  Vitest + Testing Library tests; ESLint + Prettier; independent CI + image.

## M7 — Repository Code Chat (RAG) — FLAGSHIP — 2026-07

### Added
- **`conversation` module** — the flagship Repository Code Chat. Chat sessions +
  turns (`chat_sessions`/`chat_turns`, Flyway `V10`; citations as JSON TEXT),
  org/repo-scoped, per user.
- **RAG orchestration** (`RagChatService`): question → hybrid retrieval
  (`retrieval :: search`) → token-budgeted, guardrail-fenced prompt → streamed
  generation (`ai :: chat`) → answer with **citations to file:line derived from
  the retrieved chunks** (ADR-0014). Windowed conversation memory (ADR-0013).
- **SSE streaming endpoint** `POST /api/v1/chat/sessions/{id}/messages` →
  `token` events, then `citations`, then `done`; assistant turn persisted on
  completion. Session CRUD endpoints; org + session-ownership authorization.
- **Prompt-injection guardrail**: retrieved repo content fenced as untrusted data.
- ADR-0013 (windowed memory), ADR-0014 (citations-from-retrieval + guardrail).
  13 new tests (prompt assembly, RAG orchestration incl. memory, SSE) — no Docker.

### Changed
- App config adds `aicodeassistant.conversation` tuning (retrieval limit, memory
  window, context char budget).

## M6 — AI Orchestration (Chat) — 2026-07

### Added
- **`ChatModel` provider abstraction** (`ai` module, `chat` named interface):
  provider-neutral `ChatMessage`/`ChatRequest`/`ChatResponse`/`ChatToken`; two
  real implementations selected by `aicodeassistant.ai.chat.provider`:
  - **Ollama** (default, offline): `POST /api/chat`, NDJSON streaming.
  - **OpenAI** (opt-in): `POST /v1/chat/completions` (Bearer key), SSE streaming.
- **Token streaming** in the port from day one (`chatStream()` → closeable
  `Stream<ChatToken>`); browser SSE wiring lands in M7.
- Resilience4j retry + circuit breaker + timeouts per provider (`ollama-chat`,
  `openai-chat`; transient-only, credential rejection never retried); token usage
  captured as `ai.chat.tokens` metrics (observability, not billing).
- ADR-0012 (chat provider abstraction). 12 new tests: both providers' blocking +
  streaming + failure paths via WireMock (no Docker).

### Changed
- `OllamaProperties` gained `chatModel`; new `OpenAiProperties`. App config adds
  `aicodeassistant.ai.chat.provider` + OpenAI settings; the vestigial M0
  `ai.provider` key was replaced.

## M5 — Retrieval (Hybrid Search) — 2026-07

### Added
- **Hybrid code search** (`retrieval` module, `search` named interface):
  `CodeSearch` / `SearchQuery` / `SearchResult`. Combines pgvector cosine KNN and
  Postgres full-text search, fused with **reciprocal-rank fusion**; results carry
  file:line provenance and a `VECTOR`/`LEXICAL`/`HYBRID` source. Org-scoped;
  degrades to lexical-only if embeddings are unavailable.
- **Postgres FTS** for lexical search (generated `content_fts tsvector` + GIN
  index, Flyway `V9`) — no OpenSearch, no dual-write (ADR-0011).
- `POST /api/v1/search` endpoint, authorized via iam `OrganizationAccess.canRead`.
- ADR-0011 (Postgres FTS over OpenSearch). 9 new tests (RRF correctness incl.
  hybrid > vector-only, controller authz/mapping) run without Docker; a
  Testcontainers `HybridSearchIT` exercises real pgvector + FTS in CI.

### Changed
- `retrieval` now depends on `ai :: embedding` and `iam :: api`; the
  `indexer-worker` scans only `retrieval.chunk` (write side), keeping the search
  REST controller out of the headless worker.

## M4 — Indexing Worker & Saga — 2026-07

### Added
- **Indexing saga** (`indexing` module, run by `indexer-worker`): consumes
  `repository.import-requested` and runs claim → clone → parse → chunk → embed →
  upsert → INDEXED, with compensation (delete partial vectors, mark FAILED, emit
  `repository.indexing-failed`) and idempotent re-index. `IndexJob` aggregate +
  `index_jobs` table (Flyway `V7`).
- **Exactly-once via an atomic DB status-claim** (conditional UPDATE) instead of
  a distributed lock (ADR-0009); Redisson stays deferred.
- **Ollama embedding client** (`ai` module, `embedding` named interface):
  `POST /api/embed`, Resilience4j retry + circuit breaker + timeouts,
  `ai.embedding.call` metric; provider DTOs stay internal. No provider
  abstraction (Ollama-only for now).
- **pgvector chunk store** (`retrieval` module, `chunk` named interface):
  `ChunkVectorStore` write side via `JdbcTemplate`; `code_chunks` with a
  `vector(768)` column + HNSW cosine index (Flyway `V8`).
- **JGit cloner:** shallow single-branch clone to a temp dir; binary/oversized
  files skipped; working directory always cleaned up.
- **Line-based chunker** (windows + overlap, language-by-extension), ADR-0010.
- `repository` context consumes indexing-outcome events → Repository READY/FAILED.
- ADR-0009 (atomic claim), ADR-0010 (text chunking). 20 new tests (saga, chunker,
  local JGit clone, Ollama WireMock) — all run without Docker.

### Changed
- `platform` kernel gained no new deps; `indexer-worker` now runs JPA (validates
  the app-owned schema) + an embedded-Kafka-backed context test.

## M3 — Repository Context & GitHub Integration — 2026-07

### Added
- **GitHub integration** (`integration` module): OAuth 2.0 web flow (authorize +
  callback), `GitHubGateway` public API returning neutral `GitHubRepo`, and a
  `RestClient`-based API client. Provider DTOs, OAuth, and the token stay in
  `github.internal` (a Modulith named interface); nothing GitHub-specific leaks.
- **Resilience4j** circuit breaker + retry + timeouts around GitHub calls;
  retries/breaker trip only on transient failures. Per-call Micrometer metrics
  (`github.api.call`) and structured WARN logs. (Deferred from M2 per ADR-0006.)
- **AES-256-GCM `EncryptionService`** in `platform` for recoverable secrets;
  GitHub tokens encrypted at rest (`github_connections`).
- **`repository` context:** register/import repositories into an org, list
  imported repos; `Repository` aggregate with import lifecycle.
- **`RepositoryImportRequested`** domain event, externalized to Kafka via the
  transactional outbox (triggers the M4 saga).
- **`iam` public `api`** named interface: `OrganizationAccess` (intent-based
  org authorization) so the `Role` type never leaves iam.
- Flyway `V5__github_connections.sql`, `V6__repositories.sql`.
- ADR-0007 (GitHub dependency + failure modes), ADR-0008 (token encryption).
- WireMock-based external-integration tests + H2 import-flow e2e (run without Docker).

### Changed
- `platform-web` now exposes `spring-security-core` as `api` (used by every
  web-facing context's controllers + the rate-limit filter).
- GitHub HTTP client pins HTTP/1.1 (avoids a JDK-client HTTP/2 RST_STREAM edge case).

## M2 — Security Platform — 2026-07

### Added
- **API keys** (`aca_<prefix>.<secret>`, SHA-256-hashed secret, scopes, expiry,
  revocation); `X-API-Key` authentication alongside JWT. Flyway `V3`.
- **Redis token-bucket rate limiting** (atomic Lua, no locks), tiered by
  principal, `429` + `Retry-After`; **fails open** on Redis outage (ADR-0005).
- **Audit log** (`audit` module, append-only `audit_events`, Flyway `V4`) fed by
  a neutral `AuditSignal` published from the shared kernel.
- ADR-0005 (rate-limit fail-open), ADR-0006 (defer locking + Resilience4j).

## M1 — Identity & Access — 2026-07

### Added
- Registration, login, logout; **JWT** access tokens (HS256) + Postgres-backed
  **refresh-token families** with rotation and reuse detection; refresh delivered
  as a Secure/HttpOnly/SameSite cookie. BCrypt-12 passwords.
- Users, organizations, memberships; `OWNER > ADMIN > MEMBER > VIEWER` RBAC.
- First fully hexagonal bounded context (domain / application / adapters).
- Flyway `V2__iam.sql`.

## M0 — Foundation & Platform Baseline — 2026-07

### Added
- Gradle multi-module build (Java 21, convention plugins, version catalog),
  Spring Boot 3 + Spring Modulith modular monolith + independent indexing worker.
- Shared kernel: RFC-9457 error model, correlation-id plumbing, domain-event and
  transactional-outbox contracts, framework-neutral pagination.
- Observability (Prometheus/Grafana/Loki/Tempo), docker-compose stack, GitHub
  Actions CI, Flyway `V1` baseline (pgvector + outbox table).
- ADR-0001 (modular monolith), 0002 (platform/platform-web split), 0003 (Ollama
  default), 0004 (transactional outbox).
