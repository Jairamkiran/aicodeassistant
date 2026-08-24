# 0006. Defer distributed locking and Resilience4j to their first real consumer

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

The M2 roadmap listed five items: API keys, rate limiting, audit log,
**distributed locking (Redisson)**, and **Resilience4j circuit-breaker/retry
wrappers**. During the pre-milestone review it became clear that two of these
have no consumer at M2:

- **Distributed locking** was justified by "the indexing saga needs it" — but the
  saga is M4. Nothing in M2 (rate limiting uses atomic Redis ops, not locks; API
  keys and audit have no cross-instance critical section) requires a lock.
- **Resilience4j** protects calls to failure-prone external dependencies. M2 has
  none: GitHub arrives in M3, Ollama in M6.

Building either now would produce infrastructure with zero callers — the exact
"speculative generality" the project's review directive rejects.

## Decision

Defer both:

- **Distributed locking → M4**, built against the real per-repository indexing
  lock (a genuine concurrency hazard: two workers must not index the same repo
  concurrently).
- **Resilience4j → M3**, introduced around the first real external call (GitHub),
  where a circuit breaker and retry have something to protect.

The one real resilience concern in M2 — Redis being unavailable for the rate
limiter — is handled directly and simply by a fail-open policy (see ADR-0005),
not by pulling in a circuit-breaker framework prematurely.

## Consequences

- **Positive:** M2 ships three fully-consumed features and no dead code; locking
  and resilience get designed against concrete requirements instead of guesses.
- **Negative:** the résumé "breadth" of those two patterns lands a milestone or
  two later than originally sketched. Accepted — judgment and simplicity are the
  signal, and both patterns still appear, just where they're justified.

## Alternatives considered

- **Build all five in M2 as listed.** Rejected: dead code, and it models the
  wrong engineering behaviour.
- **Build a lock utility now for "future" use.** Rejected: an API with no caller
  cannot be validated and tends to be wrong when the real need finally arrives.
