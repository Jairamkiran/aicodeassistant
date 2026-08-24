/*
 * indexer-worker — the SECOND deployable. Consumes Kafka topics and runs the
 * long-running indexing saga (clone -> parse -> chunk -> embed -> upsert).
 *
 * Deliberately depends on ONLY platform + indexing. It has no web surface and
 * no knowledge of the other contexts. This narrow dependency set is what makes
 * the "extract a service when it earns its own scaling profile" story real
 * rather than aspirational — the seam already exists at the build graph level.
 */
plugins {
    id("aicodeassistant.spring-boot-app-conventions")
}

dependencies {
    implementation(project(":indexing"))

    // Embedded servlet container for ACTUATOR ONLY (K8s probes + Prometheus).
    // The worker registers no business controllers and does NOT depend on
    // :platform-web, so the RFC-9457 advice / springdoc / business web layer
    // are absent — the container serves health + metrics endpoints only.
    implementation(libs.spring.boot.starter.web)

    // Worker consumes Kafka and persists saga state.
    implementation(libs.spring.kafka)

    // Persistence: index_jobs + code_chunks live in Postgres; Flyway runs the
    // shared migrations. (The worker shares the schema with the app.)
    implementation(libs.spring.boot.starter.data.jpa)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.h2)

    "integrationTestImplementation"(libs.testcontainers.junit)
    "integrationTestImplementation"(libs.testcontainers.kafka)
    "integrationTestImplementation"(libs.awaitility)
}
