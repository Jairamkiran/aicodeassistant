# 0017. In-process Caffeine cache for the hot authorization read

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

Every authorized request resolves the caller's role in an organization
(`OrganizationAccess.canRead/…`), which hits the membership table. Under load
that is one DB round-trip per request on the hottest path. M10 (performance)
calls for caching hot reads. Redis is already in the stack (rate limiting), so
it was the obvious candidate — but a Redis lookup is itself a network round-trip
and a new failure mode on the request path.

## Decision

Cache the membership-role lookup **in-process with Caffeine**, not Redis:

- A dedicated `MembershipLookup` bean carries the `@Cacheable` (self-invocation
  would bypass the proxy), keyed by `(userId, organizationId)`, caching the
  `Optional<Role>` so "not a member" is cached too.
- A single Caffeine `CacheManager` (app `CacheConfig`) with a short TTL
  (60s expire-after-write) and a bounded size.
- Membership mutations evict the affected entry immediately, so staleness is
  bounded to seconds and correctness is preserved.

## Consequences

- **Positive:** no network hop on the request path; cannot fail the request if a
  cache backend is down; trivially testable without Docker. Order-of-magnitude
  fewer membership queries under load.
- **Negative:** per-instance cache — the same entry may be cached separately on
  each replica, and an eviction on one instance does not evict others (a foreign
  entry can be up to the TTL stale on another instance). Acceptable for a
  coarse read-authorization check with a 60s bound; a role change takes effect
  within the TTL everywhere.

## Alternatives considered

- **Redis cache.** Rejected for this path: adds a network round-trip and a
  failure mode to authorize *every* request; Redis stays the right tool for
  genuinely cross-instance state (rate-limit buckets), not a per-request read cache.
- **No cache.** Rejected: the membership read is the single most frequent query
  and caching it is a large, low-risk win.
