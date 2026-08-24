/*
 * platform-web — the web adapter kernel.
 *
 * Cross-cutting HTTP infrastructure shared by web-facing deployables: the
 * RFC-9457 @RestControllerAdvice, the correlation-id servlet filter, and their
 * auto-configuration. Depends on the servlet stack, so ONLY web deployables
 * (currently :app) depend on it — the indexer-worker does not, keeping it lean.
 */
plugins {
    id("aicodeassistant.java-library-conventions")
}

dependencies {
    api(project(":platform"))

    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)

    // Redis backs the distributed rate limiter (M2). Optional at runtime: if no
    // Redis is configured/reachable the limiter fails OPEN (see ADR-0005).
    implementation(libs.spring.boot.starter.data.redis)

    // spring-security-core (not the full starter), exposed as api: the rate-limit
    // filter and every web-facing context's controllers read the authenticated
    // principal (Authentication/SecurityContextHolder) from the SecurityContext.
    api("org.springframework.security:spring-security-core")

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.awaitility)
    "integrationTestImplementation"(libs.testcontainers.junit)
}

