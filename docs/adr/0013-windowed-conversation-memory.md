# 0013. Windowed conversation memory (defer summarization)

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

RAG chat needs conversation memory so follow-up questions have context. Two
common strategies: include the last N turns verbatim (windowed), or keep a
rolling LLM-generated summary of older turns plus recent turns verbatim.

## Decision

Use **windowed memory**: include the last N turns (default 6) verbatim in the
prompt, oldest dropped. Implemented in the `ChatSession` aggregate
(`recentTurns(n)`) — a pure, deterministic domain method. Summarization is
deferred until long sessions demonstrably need it.

## Consequences

- **Positive:** deterministic and trivially unit-testable; no extra LLM call per
  turn (no added latency, cost, or failure path); covers the large majority of
  chat sessions. Window size is configurable
  (`aicodeassistant.conversation.memory-window-turns`).
- **Negative:** very long conversations lose early context once it falls out of
  the window. Acceptable for M7; if real usage shows long sessions matter, a
  rolling summary can be added behind the same `recentTurns`/prompt seam.

## Alternatives considered

- **Windowed + rolling summary.** Rejected for M7: adds a per-turn LLM call
  (cost/latency/failure) and non-determinism that complicates testing, for a
  benefit only long sessions realise. Revisit with usage data.
- **Whole-history in the prompt.** Rejected: unbounded prompt growth blows the
  context window and cost.
