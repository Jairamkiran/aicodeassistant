# 0004. Transactional outbox for cross-boundary events

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

Bounded contexts communicate via domain events, and some events must cross the
process boundary to the `indexer-worker` over Kafka. The naïve approach — save
state to the database, then publish to Kafka — has a well-known failure mode: if
the process dies between the two operations, state and events diverge (a lost or
duplicated event). This "dual write" problem silently corrupts event-driven
systems.

## Decision

Use the **transactional outbox pattern** via Spring Modulith's event
externalization. Externalized events are persisted into an `event_publication`
table **in the same database transaction** as the state change (schema owned by
Flyway migration `V1`), then relayed to Kafka by Modulith. Delivery is
**at-least-once**; consumers must be **idempotent on `eventId`**.

## Consequences

- **Positive:** no dual write; events and state commit atomically; durable,
  replayable event log; failed relays are retried from the outbox.
- **Negative:** at-least-once delivery pushes idempotency onto consumers; the
  outbox table needs periodic archival/pruning (Modulith provides an archiving
  auto-config; retention policy to be tuned in M4). Accepted as the standard cost
  of correct event delivery.

## Alternatives considered

- **Direct publish after commit (`@TransactionalEventListener(AFTER_COMMIT)`
  → KafkaTemplate).** Rejected for cross-boundary durability: a crash after
  commit but before publish loses the event.
- **Change Data Capture (Debezium).** Deferred: heavier operational footprint
  than justified now; the outbox gives the same guarantee in-process. May revisit
  at larger scale.
