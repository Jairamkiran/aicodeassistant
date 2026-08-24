# Architecture Decision Records

This directory records the significant architectural decisions for the project,
using lightweight [MADR](https://adr.github.io/madr/)-style records. Each ADR is
immutable once accepted; a superseding decision gets a new ADR that references
the old one.

| ADR | Title | Status |
| --- | --- | --- |
| [0001](0001-modular-monolith-over-microservices.md) | Modular monolith over microservices | Accepted |
| [0002](0002-platform-and-platform-web-split.md) | Split the shared kernel into `platform` and `platform-web` | Accepted |
| [0003](0003-local-ollama-default-with-provider-port.md) | Local Ollama as the default AI provider behind a port | Accepted |
| [0004](0004-transactional-outbox-for-events.md) | Transactional outbox for cross-boundary events | Accepted |
| [0005](0005-rate-limiter-fail-open.md) | Rate limiter fails open on Redis outage | Accepted |
| [0006](0006-defer-locking-and-resilience4j.md) | Defer distributed locking &amp; Resilience4j to first real consumer | Accepted |
| [0007](0007-github-integration.md) | GitHub as an external dependency (OAuth + REST) | Accepted |
| [0008](0008-aes-gcm-token-encryption.md) | AES-256-GCM for provider tokens at rest | Accepted |
| [0009](0009-atomic-status-claim-over-distributed-lock.md) | Atomic DB status-claim instead of a distributed lock | Accepted |
| [0010](0010-text-chunking-over-ast.md) | Line-based text chunking for M4 (defer AST) | Accepted |
| [0011](0011-postgres-fts-over-opensearch.md) | Postgres FTS over OpenSearch for lexical retrieval | Accepted |
| [0012](0012-chat-model-provider-abstraction.md) | ChatModel provider abstraction (Ollama + OpenAI) | Accepted |
| [0013](0013-windowed-conversation-memory.md) | Windowed conversation memory (defer summarization) | Accepted |
| [0014](0014-citations-from-retrieval-and-injection-guardrail.md) | Citations from retrieval; prompt-injection guardrail | Accepted |
| [0015](0015-provider-agnostic-structured-output.md) | Provider-agnostic structured output (prompt + parse) | Accepted |
| [0016](0016-frontend-react-vite-mui.md) | Frontend stack: React + TypeScript + Vite + MUI + TanStack Query | Accepted |
| [0017](0017-caffeine-cache-for-authorization.md) | In-process Caffeine cache for the hot authorization read | Accepted |
| [0018](0018-notifications-via-kernel-signal.md) | In-app notifications via a neutral kernel signal | Accepted |

## Format

```
# <number>. <title>
Status · Context · Decision · Consequences · Alternatives considered
```
