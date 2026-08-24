/*
 * codeintel bounded context (DDD module) — Code Intelligence.
 *
 * Owns AI-assisted, structured code review (M9): retrieve grounding code
 * (retrieval :: search) -> prompt for a JSON review (ai :: chat + StructuredOutputs)
 * -> parse into domain findings. Org authorization via iam :: api.
 */
plugins {
    id("aicodeassistant.spring-library-conventions")
}

dependencies {
    implementation(project(":platform-web")) // REST controller + security-core
    implementation(project(":retrieval")) // retrieval :: search (CodeSearch)
    implementation(project(":ai")) // ai :: chat (ChatModel, StructuredOutputs)
    implementation(project(":iam")) // iam :: api (OrganizationAccess)

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.modulith.starter.test)
}
