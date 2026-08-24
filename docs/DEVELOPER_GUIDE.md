# Developer Guide

## Prerequisites

- **JDK 21** (the Gradle toolchain auto-provisions one if absent).
- **Node 22+** for the frontend (`frontend/`).
- **Docker** for the infra stack and Testcontainers integration tests.
- Use the bundled Gradle wrapper (`./gradlew`) — no local Gradle needed.

## Repository layout

```
platform/          Servlet-free shared kernel (errors, events, ids, signals, crypto)
platform-web/      Web adapter kernel (RFC-9457 handler, correlation + security-headers filters)
iam/ repository/ retrieval/ indexing/ ai/ conversation/ codeintel/ integration/
notification/ audit/ analytics/ sdk/   Bounded contexts (one Gradle module each)
app/               Modular-monolith deployable (aggregates all contexts)
indexer-worker/    Headless indexing-pipeline deployable
frontend/          React + TypeScript + Vite SPA (separate npm project)
buildSrc/          Gradle convention plugins
config/            Checkstyle + PMD rulesets
docker/            Observability + frontend nginx configs
k8s/               Kubernetes manifests
docs/              Architecture, roadmap, ADRs, per-milestone + these guides
```

## Backend workflow

```bash
# Compile, unit test, Spotless, Checkstyle, PMD, and Modulith verification — no Docker.
./gradlew clean check

# Auto-fix formatting when Spotless complains.
./gradlew spotlessApply

# Testcontainers integration tests (needs Docker; runs in CI).
./gradlew integrationTest
```

Quality gates enforced by `check`:

- **Spotless** (google-java-format) — formatting. `-Werror` on javac.
- **Checkstyle** — style rules (`config/checkstyle`), zero warnings.
- **PMD** — a focused defect ruleset (`config/pmd/ruleset.xml`), main sources only.
- **Spring Modulith** `ApplicationModules.verify()` — module boundaries; a
  cross-context reference not declared in `allowedDependencies` fails the build.

### Architecture rules

- Bounded contexts talk **only** through Modulith **named interfaces** (e.g.
  `iam :: api`, `ai :: chat`, `retrieval :: search`), never each other's
  internals. Cross-cutting facts travel as neutral kernel signals
  (`AuditSignal`, `NotificationSignal`).
- Contexts follow hexagonal layering: `domain` (pure) → `application`
  (use cases) → `adapter.*` (REST/JPA/HTTP). Provider types never leak out of a
  module's adapters.
- New cross-boundary events use the transactional outbox (`@Externalized`).
- Add a dependency to the version catalog (`gradle/libs.versions.toml`) and
  reference it as `libs.*`; do not hand-pin versions the BOM governs.

## Frontend workflow

```bash
cd frontend
npm install
npm run dev        # Vite dev server at :5173, proxies /api to :8080
npm run build      # tsc + production bundle
npm run test       # Vitest
npm run lint       # ESLint (max-warnings 0)
npm run format     # Prettier
```

The API client (`src/api/`) mirrors the backend DTOs; keep the two in sync when
a contract changes. SSE chat is handled by `src/api/chatStream.ts`.

## Testing strategy

- **Unit** tests use in-memory fakes — no Docker (saga, prompt assembly, RAG
  orchestration, ranking/eval, caching behaviour, structured-output parsing).
- **WireMock** covers external HTTP (GitHub, Ollama, OpenAI) including failure
  matrices.
- **Testcontainers** integration tests exercise real Postgres (pgvector + FTS) +
  Kafka in CI.
- The frontend uses Vitest + Testing Library.

## Adding a migration

Add `app/src/main/resources/db/migration/V<n>__<name>.sql` (Postgres). Keep
entities portable so app slice tests run on H2 (`ddl-auto` there); Postgres-only
SQL (pgvector, tsvector, partial/functional indexes) is exercised by
Testcontainers. Never edit an applied migration.

## Observability

Every feature ships metrics + traces + structured logs. New Micrometer timers
should get a histogram entry under `management.metrics.distribution` and, if
useful, a Grafana panel in `docker/observability/grafana/dashboards`.
