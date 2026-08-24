# 0002. Split the shared kernel into `platform` and `platform-web`

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

Every module depends on a shared kernel for cross-cutting concerns (error model,
events, pagination, correlation id, clock). The `indexer-worker` deployable is
headless business-wise but still needs the kernel. If the kernel pulls in the
servlet web stack, the worker inherits an embedded web server and the full MVC
machinery it does not use for business purposes — bloating its image and blurring
the "lean worker" story.

## Decision

Split the kernel in two:

- **`platform`** — servlet-free core. Depends on `spring-web` only for the
  `ProblemDetail`/`HttpStatus` *types* (a plain library, no embedded server), so
  the error model is shareable without dragging in Tomcat. Safe for the worker.
- **`platform-web`** — web adapter kernel: the RFC-9457 `@RestControllerAdvice`,
  the correlation-id servlet filter, and their auto-configuration. Only web
  deployables (`app`) depend on it.

Packages are disjoint (`...platform.*` vs `...platform.web.*`), so there is no
split-package problem.

## Consequences

- **Positive:** the worker's dependency graph is honestly minimal; the core is
  unit-testable without a servlet container; a reusable web-kernel seam exists
  for any future web service.
- **Negative:** two modules instead of one, and a small judgement call about what
  belongs where. Accepted — the boundary is clear (does it touch the servlet
  API?).

## Alternatives considered

- **Single kernel module with the web stack.** Rejected: forces Tomcat into the
  worker and couples core error types to a running web server.
- **Duplicate the error types in each deployable.** Rejected: violates DRY and
  drifts over time.
