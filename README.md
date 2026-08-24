# AI Software Engineering Assistant

> An enterprise-grade AI assistant for software teams: understand repositories,
> chat over code with citations, review pull requests, generate tests, analyse
> stack traces, and more — built on a clean, event-driven, modular architecture.

[![CI](https://github.com/Jairamkiran/aicodeassistant/actions/workflows/ci.yml/badge.svg)](https://github.com/Jairamkiran/aicodeassistant/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)

AI Software Engineering Assistant is built the way a real product is built: strict DDD bounded contexts,
hexagonal ports/adapters, a transactional-outbox event bus, an independently
scalable indexing worker, and full observability — all runnable locally, for
free, against a local Ollama model.

---

## Status

**Milestones 0–13 are complete** — a full-stack, production-ready platform: from
the foundation through the flagship Repository Code Chat, and on through the web
client, structured code review, performance/observability, platform features,
and container/Kubernetes deployment.

| Capability | State |
| --- | --- |
| Gradle multi-module build (Java 21, convention plugins, version catalog) | ✅ |
| Modular monolith (`app`) + independent indexing worker (`indexer-worker`) | ✅ |
| Shared kernel: RFC-9457 errors, correlation ids, events, outbox, AES-GCM crypto, neutral signals | ✅ |
| Spring Modulith boundary verification + auto-generated architecture docs | ✅ |
| **Identity & Access:** register/login, JWT + rotating refresh (reuse detection), users/orgs/memberships, RBAC | ✅ |
| **Security Platform:** API keys (hashed, scoped), Redis token-bucket rate limiting (fail-open), append-only audit log | ✅ |
| **Repository & GitHub:** OAuth linking (encrypted tokens), import/reindex/delete lifecycle, Resilience4j-guarded client | ✅ |
| **Indexing Saga:** Kafka-driven clone→chunk→embed→pgvector, atomic status-claim, compensation, stalled-job reaper | ✅ |
| **Hybrid Search:** pgvector cosine + Postgres FTS fused with weighted reciprocal-rank fusion, language filter, org-scoped | ✅ |
| **AI Orchestration:** `ChatModel` port, Ollama + OpenAI providers, token streaming, provider-agnostic structured output | ✅ |
| **Repository Code Chat (RAG):** grounded SSE-streamed answers with file:line citations, windowed memory, injection guardrail | ✅ |
| **Code Intelligence:** structured AI code review (severity/file/line findings) grounded in retrieved code | ✅ |
| **Web UI:** React + TypeScript + Vite + MUI + Monaco — auth, dashboard, live indexing, search, streaming chat, code review, settings | ✅ |
| **Platform:** in-app notifications, background jobs, Caffeine authorization cache, search/RAG latency metrics | ✅ |
| **Production:** PMD, non-root Docker images, Kubernetes manifests (probes/HPA/ingress), security headers, prod profile | ✅ |
| Unit + slice + WireMock tests (backend) · Vitest + Testing Library (frontend) | ✅ |
| Testcontainers integration (Postgres+Kafka; iam Postgres; Redis rate-limiter) | ✅ CI¹ |
| `docker compose` stack + apps profile (app, worker, frontend) | ✅ CI¹ |
| Observability: Prometheus, Grafana, Loki, Tempo (OTel) | ✅ CI¹ |
| GitHub Actions CI (backend build/test/PMD, frontend build/test/lint, integration, images) | ✅ |

¹ *Docker-dependent items (`docker compose up`, Testcontainers integration
tests) run in CI and on any machine with a Docker daemon; they are not executed
on the Docker-less development machine used to author the code.*

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the milestone breakdown.

---

## Architecture at a glance

```
   ┌────────────────────────┐
   │  frontend (React SPA)  │  nginx: static assets + /api reverse proxy
   └───────────┬────────────┘
               │ REST + SSE
               ▼
   ┌──────────────────────────────────────────────┐
   │                 app (monolith)                 │
   │  iam · repository · retrieval · conversation   │
   │  ai · codeintel · integration · notification   │
   │  audit · analytics · sdk     (Spring Modulith) │
   └───────────────┬────────────────────────────────┘
                   │ domain events (transactional outbox)
                   ▼
                 Kafka
                   │
   ┌───────────────▼────────────────────────────────┐
   │            indexer-worker (headless)            │
   │  clone → parse → chunk → embed → upsert          │
   │  (independently scalable indexing saga)          │
   └──────────────────────────────────────────────────┘

  Postgres (+pgvector) · Redis · Kafka · Ollama · Prometheus/Grafana/Loki/Tempo
```

Two deployables share one codebase. The monolith serves the synchronous API;
the worker owns the long-running, resource-heavy indexing pipeline — the one
place a separate service earns its keep. See
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full rationale.

---

## Tech stack

**Backend** Java 21 · Spring Boot 3.5 · Spring Modulith · Spring Data JPA ·
Spring Kafka · Flyway · Resilience4j · Gradle (Kotlin DSL)
**Data / infra** PostgreSQL + pgvector · Redis · Kafka (KRaft) · OpenSearch
**AI** Ollama (default, local) · pluggable OpenAI/Azure/Bedrock via a provider port
**Observability** Micrometer + Prometheus · Grafana · Loki · Tempo (OpenTelemetry)
**Testing** JUnit 5 · AssertJ · Testcontainers · Spring Modulith test

---

## Prerequisites

- **JDK 21** (the Gradle toolchain will auto-provision one if absent)
- **Node 22+** — for the `frontend/` web client
- **Docker + Docker Compose** — required for the infra stack and integration tests
- No Gradle install needed — use the bundled wrapper (`./gradlew`)

## Quick start

```bash
# 1. Build + run unit tests, style/PMD checks, and modularity verification (no Docker)
./gradlew clean check

# 2. Build, test, and lint the web client (no Docker)
cd frontend && npm install && npm run build && npm run test && npm run lint && cd ..

# 3. Boot the infrastructure + observability stack
docker compose up -d           # Postgres, Redis, Kafka, OpenSearch, Ollama, Grafana...
#   Ollama models (llama3.1, nomic-embed-text) are pulled automatically once.

# 4. Run the apps from your IDE, or everything (incl. the SPA) in containers:
docker compose --profile apps up -d --build
#    web        → http://localhost:3001
#    app        → http://localhost:8080   (Swagger UI: /swagger-ui.html)
#    worker     → http://localhost:8081/actuator/health
```

### Useful endpoints

| URL | What |
| --- | --- |
| http://localhost:8080/actuator/health | App health (liveness/readiness) |
| http://localhost:8080/actuator/prometheus | App metrics |
| http://localhost:8080/actuator/modulith | Live module structure |
| http://localhost:3000 | Grafana (anonymous viewer; admin/admin) |
| http://localhost:9090 | Prometheus |

---

## Verifying M0

Everything except the Docker-dependent steps is verified by `./gradlew check`.
To verify the remaining DoD items on a machine with Docker:

```bash
# Testcontainers integration test — spins real Postgres(pgvector)+Kafka, runs
# Flyway migrations, round-trips a Kafka message.
./gradlew integrationTest

# Full stack boots and Grafana shows app metrics.
docker compose up -d
docker compose --profile apps up -d --build
open http://localhost:3000
```

CI (`.github/workflows/ci.yml`) runs all of the above on every push/PR.

---

## Repository layout

```
platform/          Servlet-free shared kernel (errors, events, ids, signals, crypto)
platform-web/      Web adapter kernel (RFC-9457 handler, correlation + security-headers filters)
iam/ repository/ …  Bounded contexts (one Gradle module each)
app/               Modular-monolith deployable (aggregates all contexts)
indexer-worker/    Headless indexing-pipeline deployable
frontend/          React + TypeScript + Vite SPA (separate npm project)
buildSrc/          Gradle convention plugins (java-library, spring-library, spring-boot-app)
config/            Checkstyle + PMD rulesets
docker/            Observability configs + frontend nginx config
k8s/               Kubernetes manifests (deployments, HPA, ingress)
docs/              Architecture, roadmap, ADRs, deployment/config/developer/API guides
```

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — system design, contexts, events, trade-offs
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — Docker Compose + Kubernetes deployment
- [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md) — every configuration key & default
- [`docs/DEVELOPER_GUIDE.md`](docs/DEVELOPER_GUIDE.md) — build, test, and architecture rules
- [`docs/API.md`](docs/API.md) — REST/SSE endpoint reference (OpenAPI at `/v3/api-docs`)
- [`docs/adr/`](docs/adr/README.md) — Architecture Decision Records (0001–0018)
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — milestone plan & definitions of done
- [`docs/milestones/`](docs/milestones/) — per-milestone deep-dives (M0–M7)
- [`CHANGELOG.md`](CHANGELOG.md) — changelog (milestone-grouped)
- [`CONTRIBUTING.md`](CONTRIBUTING.md) · [`SECURITY.md`](SECURITY.md) · [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md)

## Maintainer

**Jairamkiran Vasupalli** — [@Jairamkiran](https://github.com/Jairamkiran)

## License

Copyright © 2026 Jairamkiran Vasupalli.

Licensed under the Apache License 2.0 — see [`LICENSE`](LICENSE).
