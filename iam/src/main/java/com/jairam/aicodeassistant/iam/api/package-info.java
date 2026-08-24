/**
 * Public, cross-module API of the iam context — a Spring Modulith {@link
 * org.springframework.modulith.NamedInterface named interface}.
 *
 * <p>Types here (and only here, plus the module base package) are the sanctioned surface other
 * modules may depend on. Provider- and domain-internal types (the {@code Role} enum, aggregates,
 * JPA entities) stay inside the module.
 */
@org.springframework.modulith.NamedInterface("api")
package com.jairam.aicodeassistant.iam.api;
