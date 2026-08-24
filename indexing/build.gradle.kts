/*
 * indexing — the indexing saga (Milestone 4). Owned by the indexer-worker
 * deployable.
 *
 * Orchestrates: claim -> clone -> parse+chunk -> embed -> upsert -> mark indexed,
 * with compensation on failure. Depends on the public APIs of `ai` (embeddings)
 * and `retrieval` (vector store), consumes Kafka, and clones via JGit. It owns
 * its own index_jobs table; it never writes another context's tables — it emits
 * completion events instead.
 */
plugins {
    id("aicodeassistant.spring-library-conventions")
}

dependencies {
    implementation(project(":ai"))
    implementation(project(":retrieval"))
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.json) // Jackson ObjectMapper for the Kafka payload
    implementation(libs.spring.kafka)
    implementation(libs.jgit)

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.spring.kafka.test)

    "integrationTestImplementation"(libs.testcontainers.junit)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
    "integrationTestImplementation"(libs.testcontainers.kafka)
    "integrationTestImplementation"(libs.awaitility)
}
