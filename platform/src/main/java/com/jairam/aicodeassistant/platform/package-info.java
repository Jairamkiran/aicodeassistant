/**
 * AI Software Engineering Assistant shared kernel.
 *
 * <p>Cross-cutting primitives shared by every bounded context: the RFC-9457 error model,
 * correlation-id/observability plumbing, framework-neutral pagination, domain-event and outbox
 * contracts, typed-id and clock support.
 *
 * <p>Declared as an {@link org.springframework.modulith.ApplicationModule.Type#OPEN OPEN} module:
 * it is the shared kernel, so all of its packages (error, event, api, observability, ...) are part
 * of its public API and may be referenced by any bounded context without breaching boundaries. It
 * contains no business logic and depends on no bounded context. See ADR-0002.
 */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.jairam.aicodeassistant.platform;
