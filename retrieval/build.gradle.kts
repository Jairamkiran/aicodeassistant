/*
 * retrieval — vector store + hybrid search (Milestone 4 write side, M5 query side).
 *
 * M4 delivered the pgvector write side (ChunkVectorStore). M5 adds hybrid search:
 * pgvector cosine KNN + Postgres full-text search fused with reciprocal-rank
 * fusion, exposed via a search REST endpoint. Uses JdbcTemplate for the
 * pgvector/FTS SQL. No new infrastructure (Postgres FTS, not OpenSearch — ADR-0011).
 */
plugins {
    id("aicodeassistant.spring-library-conventions")
}

dependencies {
    implementation(libs.spring.boot.starter.data.jpa) // JdbcTemplate + datasource
    implementation(project(":platform-web")) // search REST controller
    implementation(project(":ai")) // embed the query text (ai :: embedding)
    implementation(project(":iam")) // org-scoped auth (iam :: api)
    implementation(libs.micrometer.core) // search-latency metrics

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.modulith.starter.test)

    "integrationTestImplementation"(libs.testcontainers.junit)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
}
