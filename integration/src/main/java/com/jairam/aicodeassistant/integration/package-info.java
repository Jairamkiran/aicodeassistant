/**
 * External Integrations bounded context.
 *
 * <p>Spring Modulith application module owning all third-party provider integrations (GitHub in M3;
 * Jira/Slack later). Provider-specific code — HTTP clients, JSON DTOs, OAuth, resilience, and
 * encrypted provider tokens — is encapsulated in {@code <provider>.internal} packages. Other
 * modules use only the public, domain-typed gateway APIs (e.g. {@code GitHubGateway}); provider
 * types and credentials never cross the boundary.
 */
@org.springframework.modulith.ApplicationModule(displayName = "External Integrations")
package com.jairam.aicodeassistant.integration;
