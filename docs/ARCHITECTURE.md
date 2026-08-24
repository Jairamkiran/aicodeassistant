# AI Software Engineering Assistant — Architecture

This document explains the system design and the reasoning behind each major
decision. It is the reference that milestone docs build on.

## 1. Guiding principles

1. **Judgment over pattern-count.** We use distributed-systems patterns where
   they earn their keep, not to decorate a résumé. A modular monolith with one
   genuinely-separate worker demonstrates *when* to draw a service boundary —
   the signal senior reviewers actually screen for.
2. **Boundaries enforced by the compiler.** Each bounded context is its own
   Gradle module, so illegal dependencies fail the build, not a code review.
   Spring Modulith adds runtime package-level verification on top.
3. **No dual writes.** State changes and the events announcing them commit in
   one transaction via the outbox; relay to Kafka is at-least-once.
4. **Everything observable.** Metrics, traces, and structured logs correlate
   through a single correlation id / trace id from day one.
5. **Runs locally, free.** The default AI provider is a local Ollama model, so
   anyone can clone and run the whole product offline.

## 1a. Current state vs. target architecture (read this first)

Be precise about what exists today so this document never overclaims:

- **Present now (M0–M13):** the shared kernel (`platform`), the web adapter
  kernel (`platform-web`), the two backend deployables, a React web client
  (`frontend/`), and the bounded contexts built out hexagonally through the
  feature milestones — `iam`, `repository`, `indexing`, `retrieval`, `ai`,
  `conversation`, `codeintel`, `integration`, `notification`, and `audit`.
  (`analytics` and `sdk` remain descriptor-only, reserved for later work.)
  Module boundaries are enforced by `ApplicationModules.verify()`; the
  event/outbox contracts, observability, caching, and the CI/CD + container/K8s
  packaging are real.
- **Structure:** each built context follows **hexagonal** layering — a pure
  `domain` (aggregates, value objects, ports) with no framework imports, an
  `application` layer (use-case services), and `adapters` (inbound REST/SSE,
  outbound JPA/Kafka/HTTP). Cross-context calls go only through Modulith named
  interfaces; cross-cutting facts travel as neutral kernel signals
  (`AuditSignal`, `NotificationSignal`).

Where a module is descriptor-only it is called out as such; everything else
described here is shipped.

## 2. Topology

Three deployables, one repository:

- **`app`** — the modular monolith. Synchronous request/response surface: REST +
  SSE. Aggregates all bounded contexts.
- **`indexer-worker`** — a headless deployable consuming Kafka. Runs the
  long-running indexing pipeline (clone → parse → chunk → embed → upsert). Its
  resource profile (CPU/IO heavy, bursty, independently scalable) is completely
  different from the request path, which is precisely why it is separate.
- **`frontend`** — a React + TypeScript SPA (its own npm project), served in
  production by nginx which also reverse-proxies `/api` to `app` (ADR-0016). It
  is not part of the JVM build graph.

### Why not full microservices?

Splitting `auth-service`, `user-service`, `notification-service` etc. over HTTP
on day one buys distributed-systems tax (network failures, distributed
transactions, deployment complexity) with no scaling or team-boundary benefit at
this stage. It also reads as premature decomposition. The modular monolith keeps
the *logical* boundaries crisp and cheap to extract later — and we already prove
we can extract by running the indexing worker as its own process. When a context
develops a real independent scaling or ownership need, its module lifts out with
minimal surgery because it never shared internals.

## 3. Bounded contexts (Spring Modulith modules)

| Module | Responsibility | Deployable |
| --- | --- | --- |
| `platform` | Shared kernel: errors, events, ids, pagination, clock, outbox contracts. Servlet-free. | both |
| `platform-web` | Web adapter kernel: RFC-9457 handler, correlation filter. | app only |
| `iam` | Users, orgs, memberships, auth, JWT/refresh, RBAC, API keys. | app |
| `repository` | Repo registration, GitHub OAuth linkage, import lifecycle. | app |
| `indexing` | Indexing saga domain (owned by the worker). | worker |
| `retrieval` | Vector + lexical (hybrid) search, context assembly. | app |
| `conversation` | Chat sessions, turns, memory, prompt history. | app |
| `ai` | LLM/embedding provider abstraction, prompt registry, RAG orchestration. | app |
| `codeintel` | Code review, PR review, test gen, stack-trace analysis, security scan. | app |
| `integration` | GitHub App/webhooks, Jira, Slack. | app |
| `notification` | WebSocket/email/in-app notifications, background jobs. | app |
| `analytics` | Usage metering, dashboards, billing-ready ledger. | app |
| `sdk` | Public API surface, API-key auth path, plugin framework. | app |

> Contexts marked as stubs today carry only their Modulith descriptor
> (`package-info.java`) so they participate in boundary verification. Domain,
> application, and adapter code arrives in the milestone that owns each context.

### Module communication rules

- No module references another module's `internal` package. Public API lives in
  the module's base package (and `api` sub-packages).
- Cross-context interaction is via **published domain events** or explicit
  **application-service ports**, never by reaching into internals.
- Modulith's `ApplicationModules.verify()` test fails the build on any breach or
  dependency cycle.

## 4. The shared kernel split (`platform` vs `platform-web`)

`platform` is deliberately **servlet-free**: it depends on `spring-web` (for the
`ProblemDetail`/`HttpStatus` *types*) but never on an embedded web server. This
lets the non-web worker depend on the kernel without dragging Tomcat onto its
classpath. The web-only pieces (the `@RestControllerAdvice`, the servlet filter,
their auto-configuration) live in `platform-web`, which only the `app` depends
on. This is a small decision with an outsized payoff: the worker's dependency
graph stays honest and lean.

## 5. Error model (RFC-9457)

All errors render as `application/problem+json` with a stable shape:

```json
{
  "type": "https://aicodeassistant.dev/problems/resource-not-found",
  "title": "The requested resource was not found",
  "status": 404,
  "detail": "User not found: u-123",
  "code": "resource-not-found",
  "correlationId": "0f1e...",
  "timestamp": "2026-01-01T00:00:00Z",
  "resource": "User",
  "identifier": "u-123"
}
```

`ApplicationException` subclasses fix the category + HTTP status, so controllers
never map status codes by hand. `GlobalExceptionHandler` (in `platform-web`) is
the single translation point; it logs 4xx at WARN (expected) and 5xx at ERROR
with the stack trace, and never leaks internal exception messages to clients.

## 6. Events & the transactional outbox

Domain events implement `DomainEvent` (unique id + occurrence time). They are
published in-process via Spring's `ApplicationEventPublisher`. Events that must
cross to the worker or be durable are **externalized**: Spring Modulith persists
them into the `event_publication` table *in the same transaction* as the state
change (the outbox), then relays them to Kafka. This removes the dual-write race
that breaks naïve "save then publish" designs. Delivery is at-least-once;
consumers are idempotent on `eventId`.

## 7. The indexing saga (M4)

Repository import is a long-running, compensatable saga spanning both
deployables:

```
RegisterRepo → CloneRepo → ParseFiles → ChunkFiles → EmbedChunks → UpsertVectors → MarkIndexed
```

Each step is idempotent and compensatable (e.g. `EmbedChunks` fails → mark repo
`INDEXING_FAILED`, release the per-repo distributed lock, emit a DLQ event,
surface the reason to the UI). This is the legitimate distributed-transaction
story in the system and the showcase for retries, backoff, DLQ, and circuit
breakers. Full design lands with M4.

## 8. Observability

- **Metrics** — Micrometer → Prometheus. Every deployable tags metrics with
  `application`. A provisioned Grafana "Service Overview" dashboard ships in the
  repo (request rate, p95 latency, 5xx rate, JVM heap, threads, service up).
- **Traces** — Micrometer Tracing → OTLP → Tempo.
- **Logs** — structured JSON (logstash-logback-encoder) including
  `correlationId`, `traceId`, `spanId`, shipped to Loki. In `local`/`test`
  profiles logs are human-readable instead.
- **Correlation** — a highest-precedence servlet filter binds/echoes a
  correlation id per request; it appears in every log line and every error body,
  so logs ↔ metrics ↔ traces line up in Grafana.

## 9. Build system

- Gradle (Kotlin DSL), **convention plugins** in `buildSrc` (not
  `subprojects {}`, which breaks project isolation and the configuration cache).
- A **version catalog** (`gradle/libs.versions.toml`) + Spring Boot / Modulith /
  Resilience4j **BOMs** align versions so leaf modules declare dependencies
  without version strings.
- Three conventions: `java-library` (base: Java 21, tests, Spotless,
  Checkstyle, `-Werror`, a separate `integrationTest` source set),
  `spring-library` (contexts), `spring-boot-app` (the two deployables).
- **`-Werror`** is on: warnings fail the build, keeping the tree clean.

## 10. Testing strategy

- **Unit / slice tests** run with `./gradlew test` and need no Docker. The app's
  `ContextLoadsTest` boots the full context with infra auto-config excluded; the
  worker asserts the business web layer is absent (proving its lean footprint).
- **Modularity test** (`ApplicationModules.verify()`) enforces boundaries and
  emits C4 PlantUML + AsciiDoc docs under `app/build/spring-modulith-docs`.
- **Integration tests** run with `./gradlew integrationTest`, use Testcontainers
  (real Postgres+pgvector and Kafka), and are excluded from the fast unit gate.
  They run in CI where a Docker daemon is available.

## 11. Security posture (grows per milestone)

M0 establishes the seams: sanitised error bodies, no secrets in code (env-var
config with dev-only fallbacks), non-root container users, bounded pagination.
Authentication/authorization (JWT + refresh rotation, RBAC), API keys, rate
limiting, audit logging, and resilience wrappers arrive in M1–M2.
