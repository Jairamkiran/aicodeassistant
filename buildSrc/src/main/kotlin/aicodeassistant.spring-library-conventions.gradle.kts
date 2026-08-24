/*
 * Convention for bounded-context modules (iam, repository, ...). These are
 * libraries, NOT bootable apps — they must not carry a main() or produce a fat
 * jar. They get the Spring context, validation, AOP, Modulith core, and
 * observability primitives so any context can publish events, expose metrics,
 * and declare @ApplicationModule metadata.
 */
plugins {
    id("aicodeassistant.java-library-conventions")
}

dependencies {
    api(project(":platform"))

    implementation(libs.lib("spring-boot-starter").get())
    implementation(libs.lib("spring-boot-starter-validation").get())
    implementation(libs.lib("spring-boot-starter-aop").get())
    implementation(libs.lib("spring-modulith-starter-core").get())

    annotationProcessor(libs.lib("spring-boot-configuration-processor").get())

    testImplementation(libs.lib("spring-modulith-starter-test").get())
}
