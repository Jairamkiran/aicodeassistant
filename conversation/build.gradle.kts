/*
 * conversation — Repository Code Chat (Milestone 7, flagship RAG).
 *
 * Owns chat sessions/turns + the RAG orchestrator: question -> hybrid search
 * (retrieval :: search) -> token-budgeted, guardrail-fenced prompt -> streamed
 * chat (ai :: chat) -> answer with citations to file:line. Org/session auth via
 * iam :: api. Windowed conversation memory. SSE streaming to the client.
 */
plugins {
    id("aicodeassistant.spring-library-conventions")
}

dependencies {
    implementation(project(":platform-web")) // SSE controller + security-core
    implementation(project(":retrieval")) // retrieval :: search (CodeSearch)
    implementation(project(":ai")) // ai :: chat (ChatModel)
    implementation(project(":iam")) // iam :: api (OrganizationAccess)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.micrometer.core) // RAG answer-latency metrics

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.h2)
    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.awaitility)

    "integrationTestImplementation"(libs.testcontainers.junit)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
    "integrationTestImplementation"(libs.h2)
}
