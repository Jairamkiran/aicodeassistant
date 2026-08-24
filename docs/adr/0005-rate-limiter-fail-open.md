# 0005. Rate limiter fails open on Redis outage

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

M2 adds a distributed rate limiter backed by Redis (a token bucket enforced by
an atomic Lua script). Redis is now on the request path for every rate-limited
endpoint. We must decide what happens when Redis is slow or unreachable.

Two options: **fail closed** (reject requests when the limiter can't be
consulted) or **fail open** (allow them).

## Decision

**Fail open.** If the Redis call throws or returns an unexpected result, the
limiter logs a WARN and allows the request. A no-op limiter is also wired when
Redis is not configured at all, so a deployable always boots.

## Consequences

- **Positive:** a Redis outage degrades a protective feature rather than taking
  down the entire API. Availability of core functionality is preserved; the blast
  radius of a Redis incident is "rate limiting is temporarily not enforced," not
  "all requests 5xx."
- **Negative:** during a Redis outage the API is unprotected from abuse. This is
  an accepted, bounded risk: abuse protection is a mitigation, not a core
  guarantee, and other layers (auth, quotas, upstream WAF/CDN) remain. The WARN
  logs make the degraded state observable.

## Alternatives considered

- **Fail closed.** Rejected: turns a dependency's availability problem into a
  total outage of the product — a worse failure mode for a rate limiter, whose
  job is protection, not gatekeeping of correctness.
- **Local in-memory fallback bucket.** Rejected for M2: per-instance buckets
  don't enforce a global limit and add complexity; revisit only if abuse during
  Redis outages proves to be a real problem.
