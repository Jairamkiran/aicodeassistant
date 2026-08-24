# AI Software Engineering Assistant — Milestone Roadmap

Sequencing rule: **build the dependency before the dependent.** Each milestone
ships with migrations, DTOs/entities/mappers/exceptions/repositories, validation,
structured logging, metrics + traces, error handling, unit **and** integration
tests (green on a clean CI checkout), OpenAPI docs, and a documentation set
(architecture, sequence + class diagrams, schema, design decisions, trade-offs,
scalability/security/performance notes, testing strategy). A milestone is "done"
only when its Definition of Done passes in CI.

| # | Milestone | Objective | Status |
| --- | --- | --- | --- |
| **M0** | Foundation & Platform Baseline | Running, observable, tested, containerized skeleton. | ✅ **Complete** |
| **M1** | Identity & Access | Registration, login, JWT + rotating refresh, users/orgs, RBAC. | ✅ **Complete** |
| **M2** | Security Platform | API keys, distributed rate limiting, audit log. (Distributed locking → M4, Resilience4j → M3 — see ADR-0006.) | ✅ **Complete** |
| **M3** | Repository Context | Repo registration, GitHub OAuth, import lifecycle, import-requested event. **+ Resilience4j** (circuit breaker/retry) around the first external GitHub calls (moved from M2). | ✅ **Complete** |
| **M4** | Indexing Worker & Saga | Compensating saga: clone→parse→chunk→embed→upsert (pgvector); retry, idempotency, per-repo **atomic status-claim** (ADR-0009, chosen over a distributed lock). | ✅ **Complete** |
| **M5** | Knowledge Retrieval | Hybrid vector+lexical search (RRF) with file:line provenance; **Postgres FTS** for lexical (ADR-0011, chosen over OpenSearch). Context assembly deferred to M7. | ✅ **Complete** |
| **M6** | AI Orchestration | Provider-agnostic `ChatModel` port with two providers (Ollama default + OpenAI), token streaming, resilience, token-usage metrics (ADR-0012). Prompt registry → M7, structured output → M9. | ✅ **Complete** |
| **M7** | **Repository Code Chat (RAG)** | **Flagship:** grounded, SSE-streaming, cited chat (citations from retrieval, ADR-0014) with windowed memory (ADR-0013) + injection guardrail. | ✅ **Complete** |
| M8 | Frontend | React + TS + Vite + MUI + TanStack Query + Monaco; auth, import, live indexing, streaming chat UI with citations (ADR-0016). | ✅ **Complete** |
| M9 | AI Improvements | Provider-agnostic structured output (ADR-0015); structured **code review** (`codeintel`); weighted RRF ranking + evaluation harness. | ✅ **Complete** |
| M10 | Performance | Caffeine authorization cache (ADR-0017); search/RAG latency metrics; frontend error recovery + accessibility. | ✅ **Complete** |
| M11 | Platform | In-app notifications (ADR-0018); repository re-index/delete; session rename/delete; search language filter; scheduled cleanup + stalled-job reaper. | ✅ **Complete** |
| M12 | Production Readiness | PMD; frontend nginx image + compose service; Kubernetes manifests + HPA + ingress; security headers; `application-prod.yml`; CI frontend job. | ✅ **Complete** |
| M13 | Finalization | Docs (README, deployment/config/developer/API guides, diagrams), ADRs, changelog, dependency cleanup. | ✅ **Complete** |

## Definitions of Done (summary)

Each milestone's full DoD lives in `docs/milestones/M<n>.md`. Recurring bar:

- `./gradlew clean check` green (compile, unit tests, Spotless, Checkstyle, modularity).
- `./gradlew integrationTest` green (Testcontainers) in CI.
- New endpoints documented in OpenAPI; new schema via Flyway migration.
- Feature-level metrics + traces + structured logs present.
- Documentation set updated.
