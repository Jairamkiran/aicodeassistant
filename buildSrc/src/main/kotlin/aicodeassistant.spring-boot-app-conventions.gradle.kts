/*
 * Convention for the two DEPLOYABLES (app, indexer-worker). Adds the Spring Boot
 * plugin (bootJar, buildpack image support), actuator, and the observability
 * exporters that every deployable must expose (Prometheus metrics + OTLP traces).
 */
plugins {
    id("aicodeassistant.java-library-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":platform"))

    implementation(libs.lib("spring-boot-starter-actuator").get())
    implementation(libs.lib("micrometer-registry-prometheus").get())
    implementation(libs.lib("micrometer-tracing-bridge-otel").get())
    implementation(libs.lib("opentelemetry-exporter-otlp").get())
    implementation(libs.lib("logstash-logback-encoder").get())

    developmentOnly(libs.lib("spring-boot-devtools").get())
}

// A deployable's jar IS the bootJar; disable the plain jar to avoid ambiguity.
tasks.named<Jar>("jar") {
    enabled = false
}

// Deployables are applications, not libraries — no one consumes their sources.
// The base convention enables a sources jar (useful for libraries); disable the
// resulting task here so deployables ship only the bootJar.
tasks.matching { it.name == "sourcesJar" }.configureEach {
    enabled = false
}
