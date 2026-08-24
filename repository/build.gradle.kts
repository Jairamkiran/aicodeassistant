/*
 * repository — repository registration & import lifecycle (Milestone 3).
 *
 * Registers repositories into an organization and requests their import. It
 * depends on the integration module's PUBLIC API (GitHubGateway) for provider
 * data — it never sees GitHub DTOs or tokens. Depends on iam only via events /
 * the authenticated principal, not internals.
 */
plugins {
    id("aicodeassistant.spring-library-conventions")
}

dependencies {
    implementation(project(":platform-web"))
    implementation(project(":integration"))
    // Depends on iam's PUBLIC OrganizationAccess port (+ Role) to authorize
    // org-scoped imports — not on iam internals.
    implementation(project(":iam"))
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.json) // Jackson for Kafka payloads
    implementation(libs.spring.kafka) // consume indexing completion events

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.h2)
    testImplementation(libs.spring.modulith.starter.test)

    "integrationTestImplementation"(libs.testcontainers.junit)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
    "integrationTestImplementation"(libs.h2)
}
