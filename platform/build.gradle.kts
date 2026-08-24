/*
 * platform — the servlet-FREE shared kernel.
 *
 * Contains cross-cutting primitives every module (INCLUDING the non-web
 * indexer-worker) may depend on: error types, typed IDs, pagination DTOs,
 * clock, correlation-id holder, domain-event + outbox contracts.
 *
 * Deliberately has NO servlet / spring-web dependency, so depending on it does
 * not drag an embedded web server into the worker. The web-specific pieces
 * (RFC-9457 @RestControllerAdvice, servlet filter) live in :platform-web.
 */
plugins {
    id("aicodeassistant.java-library-conventions")
}

dependencies {
    // Core Spring context + Modulith event contracts (no web SERVER).
    api(libs.spring.boot.starter)
    api(libs.spring.modulith.starter.core)
    api(libs.spring.modulith.events.api)

    // spring-web provides HttpStatus/ProblemDetail (RFC-9457) as a plain
    // library — it does NOT bundle a servlet container, so the error model is
    // usable by the worker too without embedding Tomcat.
    api("org.springframework:spring-web")

    // Bean-validation API (annotations used on DTOs); no web server implied.
    api("jakarta.validation:jakarta.validation-api")
    // Jackson annotations used by event base types.
    api("com.fasterxml.jackson.core:jackson-annotations")

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.modulith.starter.test)
}
