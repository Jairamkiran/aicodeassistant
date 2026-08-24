# 0001. Modular monolith over microservices

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

The product spans many capabilities (auth, repositories, indexing, retrieval,
chat, integrations, notifications, analytics). A common reflex is to split each
into its own deployable service from day one. We must choose a system topology
that is credible to senior reviewers, correct for the current scale (single
team, no independent scaling needs yet), and cheap to evolve.

## Decision

Build a **modular monolith** using Spring Modulith: one deployable (`app`)
containing all bounded contexts as compiler-enforced modules, plus **one**
genuinely separate deployable (`indexer-worker`) for the indexing pipeline —
the only component whose resource profile (CPU/IO-heavy, bursty) justifies
independent scaling today.

Bounded contexts are separate Gradle modules, so illegal cross-context
dependencies fail compilation; Spring Modulith adds runtime boundary
verification. This keeps logical boundaries crisp and makes future extraction of
a context into its own service a mechanical operation rather than a rewrite.

## Consequences

- **Positive:** no distributed-transaction/latency/deploy tax before it is
  warranted; fast local boot; one codebase to navigate; boundaries are still
  real and enforced; the worker split proves we can extract a service when a real
  scaling reason appears.
- **Negative:** all monolith contexts scale together; a future extraction still
  requires care around shared transactions. Accepted, because these costs are
  hypothetical at current scale.

## Alternatives considered

- **Microservices from day one.** Rejected: premature decomposition, high
  operational surface, and a frequent tell of cargo-culted architecture.
- **Plain package-by-feature monolith (no module enforcement).** Rejected:
  boundaries erode without compiler + Modulith enforcement, and extraction later
  becomes a rewrite.
