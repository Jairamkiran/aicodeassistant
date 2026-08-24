/*
 * notification bounded context (DDD module) — in-app notifications (M11).
 *
 * Subscribes to neutral NotificationSignals from the shared kernel and persists
 * per-user notifications, exposed via REST. Delivery beyond the inbox goes
 * through a NotificationDispatcher port (logging default; SMTP-replaceable).
 * Coupled only to platform — no other bounded context's internals.
 */
plugins {
    id("aicodeassistant.spring-library-conventions")
}

dependencies {
    implementation(project(":platform-web")) // REST controller + security-core
    implementation(libs.spring.boot.starter.data.jpa)

    testImplementation(libs.h2)
    testImplementation(libs.spring.modulith.starter.test)

    "integrationTestImplementation"(libs.testcontainers.junit)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
    "integrationTestImplementation"(libs.h2)
}
