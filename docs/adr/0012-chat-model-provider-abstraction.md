# 0012. ChatModel provider abstraction (Ollama + OpenAI)

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

M6 introduces LLM chat completion, the capability the M7 RAG feature will use.
The project's standing rule (ADR-0006, integration directive) is: **do not add a
provider abstraction until a second provider or concrete use case exists.** For
embeddings (M4) that rule kept `EmbeddingClient` effectively single-impl (only
the indexing saga consumes it, only Ollama). For chat, there are two real
providers we want to support: local **Ollama** (offline default, ADR-0003) and
**OpenAI** (higher quality, opt-in). So the abstraction is now justified.

## Decision

Define a `ChatModel` port (public `chat` named interface) with **two real
implementations** selected by `@ConditionalOnProperty`
(`aicodeassistant.ai.chat.provider`: `ollama` default, `openai`):

- Domain-typed `ChatMessage` / `ChatRequest` / `ChatResponse` / `ChatToken` cross
  the boundary; provider JSON DTOs stay private to each adapter.
- **Streaming is in the port from day one** (`chat()` blocking + `chatStream()`
  returning a closeable `Stream<ChatToken>`), so M7 doesn't reshape the interface.
  Ollama streams NDJSON (one JSON object per line, last has `done:true`); OpenAI
  streams SSE (`data: {json}` lines, `data: [DONE]` sentinel). Both parsed in
  their adapter to the same `ChatToken` shape.
- Each call is wrapped with Resilience4j retry + circuit breaker (backends
  `ollama-chat` / `openai-chat`; retry transient only, ignore credential
  rejection) and timeouts, and records `ai.chat.call` + `ai.chat.tokens` metrics.

### Deferred (no consumer yet — would be speculative)

- **Prompt template registry** → M7 (the RAG prompt lives there).
- **Structured-output parsing** → M9 (code review / test-gen need it).
- **Prompt-injection guardrails** → M7 (first point untrusted repo content enters
  a prompt).
- **Cost/billing ledger** → M13. M6 captures token usage as **metrics** only —
  observability, not billing.
- **Second embedding provider** → not added; only chat has two providers today.

## Consequences

- **Positive:** provider choice is a config flip; the domain never sees vendor
  types; streaming won't require an interface change in M7; failures degrade to a
  clear domain error; token usage is observable.
- **Negative:** two adapters to maintain, and streaming via the JDK `HttpClient`
  (needed for a real `InputStream` body) is lower-level than `RestClient` — a
  deliberate trade for correct, testable streaming. OpenAI live use needs an API
  key (verified via WireMock in CI regardless).

## Alternatives considered

- **Ollama-only now.** Rejected: a "provider abstraction" with one implementation
  is the premature-abstraction smell; two providers is what earns the port.
- **Spring AI's `ChatClient` abstraction.** Considered; deferred adopting a
  framework abstraction in favour of a thin, fully-owned port we can test exactly
  and that keeps provider types out of our domain. Revisit if provider count grows.
