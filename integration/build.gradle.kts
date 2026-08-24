/*
 * integration — external provider integrations (Milestone 3: GitHub).
 *
 * Owns all provider-specific code: OAuth flows, HTTP clients, provider DTOs, and
 * resilience (Resilience4j circuit breaker + retry + timeout). Provider types
 * NEVER leak out — the module exposes a domain-typed public API (e.g.
 * GitHubGateway) and keeps JSON DTOs private to its adapters. Encrypted provider
 * tokens live here and are resolved internally, so no credential crosses a
 * module boundary.
 */
plugins {
    id("aicodeassistant.spring-library-conventions")
}

dependencies {
    // Web adapter kernel (OAuth callback controller) + persistence for connections.
    implementation(project(":platform-web"))
    implementation(libs.spring.boot.starter.data.jpa)

    // RestClient (synchronous HTTP) ships with spring-web via the web starter.
    // Resilience4j: circuit breaker + retry + time limiter around external calls.
    implementation(libs.resilience4j.spring.boot3)

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.h2)
    testImplementation(libs.wiremock)
    testImplementation(libs.spring.modulith.starter.test)

    "integrationTestImplementation"(libs.testcontainers.junit)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
    "integrationTestImplementation"(libs.h2)
}
