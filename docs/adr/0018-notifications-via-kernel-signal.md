# 0018. In-app notifications via a neutral kernel signal

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

M11 adds user-facing in-app notifications (e.g. "repository finished indexing").
Notifications are produced by several contexts (starting with `repository`) but
owned by one (`notification`). Having the notification module depend on every
producer's event types — or every producer depend on notification — would
couple modules and breach Modulith boundaries. The audit module already solved
the analogous problem with a neutral `AuditSignal` in the shared kernel.

## Decision

Reuse that proven pattern. Add a neutral `NotificationSignal` to the `platform`
shared kernel and have the `notification` context subscribe to exactly that one
type:

- Publishers emit `NotificationSignal` (recipient user, org, type, title,
  message, optional resource) via Spring's `ApplicationEventPublisher`; they
  never reference the notification module.
- `notification` listens for the signal, persists a `Notification` (Flyway
  `V11`), and exposes REST (list / unread-count / mark-read). Delivery beyond the
  in-app inbox is a `NotificationDispatcher` port — a logging implementation by
  default so no SMTP dependency is imposed; a real email adapter can replace it.
- Unlike `AuditSignal` (system-wide log), a `NotificationSignal` is addressed to
  a specific recipient user.

## Consequences

- **Positive:** notification stays coupled only to `platform`; producers stay
  ignorant of it; the module is independently extractable. Email is a drop-in
  seam with no core dependency. Consistent with the audit design.
- **Negative:** in-process events are delivered best-effort within the publishing
  transaction's context — a notification is not itself transactionally outboxed.
  Acceptable: notifications are advisory, and the durable record of the
  underlying action is the audit log + the aggregate's own state.

## Alternatives considered

- **notification depends on each producer's events.** Rejected: fan-in coupling
  that grows with every producer and breaks boundary verification.
- **Deliver over the transactional outbox/Kafka.** Rejected as over-engineering
  for advisory in-app messages; revisit if guaranteed cross-service delivery
  (e.g. email SLAs) becomes a requirement.
