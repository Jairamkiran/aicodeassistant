/**
 * Public SDK bounded context.
 *
 * <p>Spring Modulith application module. Its public API (this base package plus any {@code api}
 * sub-package) is the only surface other modules may use; everything under {@code internal} is
 * inaccessible across module boundaries and this is verified by the modularity test in the {@code
 * app} deployable.
 *
 * <p>Milestone status: STUB (M0) — descriptor established so the module participates in boundary
 * verification; domain/application/adapter code is added in the milestone that owns this context.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Public SDK")
package com.jairam.aicodeassistant.sdk;
