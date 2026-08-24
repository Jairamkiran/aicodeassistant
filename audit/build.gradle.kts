/*
 * audit — append-only audit-log bounded context (Milestone 2).
 *
 * Records security-relevant events (registration, login success/failure, API-key
 * lifecycle, rate-limit breaches) by listening to domain/application events. It
 * NEVER calls into other contexts' internals — it only consumes published events
 * — so the Modulith boundary stays clean and audit can be extracted later.
 *
 * Persistence-only context: JPA + platform, no web surface of its own (a
 * read/query endpoint can be added by an admin milestone later).
 */
plugins {
    id("aicodeassistant.spring-library-conventions")
}

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)

    testImplementation(libs.h2)
    testImplementation(libs.spring.modulith.starter.test)
}
