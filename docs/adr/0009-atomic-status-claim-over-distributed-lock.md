# 0009. Atomic DB status-claim instead of a distributed lock

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

The M4 roadmap named "distributed locking (Redisson)" to ensure two worker
instances don't index the same repository concurrently. Reviewing it, a
distributed lock is a poor fit for "process this work item exactly once" when the
work (clone + parse + embed) takes minutes:

- A Redis lock has a TTL. If indexing outlives the TTL, the lock expires and a
  second worker starts — **double processing**, the exact bug the lock was meant
  to prevent. Tuning the TTL against unpredictable clone/embed time is fragile.
- Renewing the lock (watchdog) adds a background thread and failure modes.

## Decision

Use an **atomic conditional UPDATE** on the job row as the claim:

```sql
UPDATE index_jobs SET status='CLAIMED', updated_at=? WHERE repository_id=? AND status='REGISTERED'
```

`1 row updated` = this worker won the claim; `0` = someone else owns it (or it's
already past REGISTERED). The saga proceeds only on a won claim. Combined with
idempotency (vectors deleted before upsert) and the durable saga state, redelivery
and retries are safe. Redisson stays deferred until a real lock need appears
(e.g. a singleton scheduled job) — consistent with ADR-0006.

## Consequences

- **Positive:** exactly-once acquisition with zero new infrastructure and no TTL
  to misconfigure; correctness does not depend on the job finishing within a
  timeout; the claim is transactional with the job state.
- **Negative:** a worker that dies mid-index leaves the job stuck in a non-terminal
  state (e.g. CLONING). A reaper that resets stale in-flight jobs to REGISTERED
  after a timeout is a follow-up (noted, not built — no evidence of need yet).
  Because it is DB-based, the claim requires the DB (already a hard dependency).

## Alternatives considered

- **Redisson distributed lock (as roadmapped).** Rejected for the TTL-vs-duration
  hazard above; more moving parts for a weaker guarantee here.
- **Kafka single-partition-per-repo ordering.** Doesn't prevent concurrent
  processing across consumer restarts/rebalances; more complex to reason about.
