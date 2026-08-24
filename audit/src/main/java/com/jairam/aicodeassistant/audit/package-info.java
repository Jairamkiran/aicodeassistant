/**
 * Audit bounded context.
 *
 * <p>Spring Modulith application module providing an append-only audit log of security-relevant
 * events. It subscribes to events published by other modules (via Spring's event mechanism) and
 * never invokes their internals, so it stays decoupled and independently extractable.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Audit")
package com.jairam.aicodeassistant.audit;
