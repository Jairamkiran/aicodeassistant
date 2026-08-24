/*
 * app — the modular-monolith deployable (web, REST, WebSocket, SSE).
 *
 * Aggregates every bounded context so the Spring Modulith verification test can
 * assert the boundaries across the whole system. This is where the composition
 * root and the HTTP surface live. It depends on ALL contexts but NOT on
 * indexer-worker (the worker is an independent deployable).
 */
plugins {
    id("aicodeassistant.spring-boot-app-conventions")
}

dependencies {
    // Web adapter kernel (RFC-9457 handler, correlation filter). Only web
    // deployables depend on this; the worker does not.
    implementation(project(":platform-web"))

    // Composition root: aggregate all bounded contexts.
    implementation(project(":iam"))
    implementation(project(":repository"))
    implementation(project(":indexing"))
    implementation(project(":retrieval"))
    implementation(project(":conversation"))
    implementation(project(":ai"))
    implementation(project(":codeintel"))
    implementation(project(":integration"))
    implementation(project(":notification"))
    implementation(project(":analytics"))
    implementation(project(":audit"))
    implementation(project(":sdk"))

    // Web + persistence + messaging surface for the monolith.
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Caching: Caffeine CacheManager for hot per-instance read caches (e.g. the
    // org-membership authorization lookup). Enabled via CacheConfig.
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.caffeine)

    // Transactional outbox: persist externalized events (JPA registry, our
    // V1 event_publication table) and relay them to Kafka. This is the
    // dual-write-safe event bridge from the monolith to the worker.
    implementation(libs.spring.modulith.events.jpa)
    implementation(libs.spring.modulith.events.kafka)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.kafka)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    // Modulith runtime insight (actuator endpoint + observability) + docs test.
    implementation(libs.spring.modulith.actuator)
    implementation(libs.spring.modulith.observability)

    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.spring.modulith.docs)
    testImplementation(libs.spring.kafka.test)
    // H2 lets the full context (incl. JPA repositories) boot offline in unit scope.
    testRuntimeOnly(libs.h2)
    testImplementation(libs.awaitility)

    // Integration-test-only deps (Testcontainers) live on the integrationTest set.
    "integrationTestImplementation"(libs.testcontainers.junit)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
    "integrationTestImplementation"(libs.testcontainers.kafka)
    "integrationTestImplementation"(libs.awaitility)
}
