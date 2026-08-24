/*
 * ai — AI provider integrations (Milestone 4: Ollama embeddings).
 *
 * Owns the embedding HTTP client. Built to the integration directive: resilience,
 * timeouts, metrics, no provider DTO leak. NO provider abstraction yet — Ollama
 * is the only provider; a port waits until a second one exists (M6, OpenAI).
 */
plugins {
    id("aicodeassistant.spring-library-conventions")
}

dependencies {
    // RestClient (spring-web) for the Ollama HTTP calls; resilience for them.
    implementation(libs.spring.boot.starter.web)
    implementation(libs.resilience4j.spring.boot3)

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.wiremock)
    testImplementation(libs.spring.modulith.starter.test)
}
