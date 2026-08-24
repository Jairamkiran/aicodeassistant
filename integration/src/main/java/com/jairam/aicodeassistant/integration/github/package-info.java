/**
 * Public GitHub integration API — a Spring Modulith {@link
 * org.springframework.modulith.NamedInterface named interface}.
 *
 * <p>{@code GitHubGateway} and the neutral {@code GitHubRepo} record are the only GitHub surface
 * other modules may use. Everything provider-specific (HTTP client, JSON DTOs, OAuth, encrypted
 * tokens) lives in {@code github.internal} and never crosses the boundary.
 */
@org.springframework.modulith.NamedInterface("github")
package com.jairam.aicodeassistant.integration.github;
