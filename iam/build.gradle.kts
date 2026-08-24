/*
 * iam — Identity & Access bounded context (Milestone 1).
 *
 * Hexagonal layering inside the module:
 *   domain       — pure Java: aggregates, value objects, domain events, PORTS.
 *                  No Spring / JPA / servlet imports.
 *   application  — use-case services orchestrating the domain via ports.
 *   adapter.*    — inbound (REST) and outbound (JPA, security, crypto) adapters
 *                  implementing the ports.
 *
 * This is the first context to populate the target architecture described in
 * docs/ARCHITECTURE.md §1a.
 */
plugins {
    id("aicodeassistant.spring-library-conventions")
}

dependencies {
    // Web adapter kernel: RFC-9457 handler + correlation filter for our controllers.
    implementation(project(":platform-web"))

    // Persistence + security + JWT (versions from the Spring Boot BOM).
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.validation)

    // Cache the hot org-membership lookup used on every authorized request.
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.caffeine)

    // Test: H2 lets the full auth flow run end-to-end WITHOUT Docker.
    testImplementation(libs.h2)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.modulith.starter.test)

    // Integration tests (CI, Docker) exercise the flow against real Postgres.
    "integrationTestImplementation"(libs.testcontainers.junit)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
    "integrationTestImplementation"(libs.h2)
}
