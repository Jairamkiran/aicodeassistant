/*
 * buildSrc — houses the convention plugins applied by every module.
 *
 * We depend on the external Gradle plugins as *marker* artifacts here so that
 * our precompiled-script convention plugins (in src/main/kotlin) can `apply`
 * them by id. This is the modern, project-isolation-friendly alternative to
 * `allprojects {}` / `subprojects {}` cross-configuration.
 */
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spring.dependency.management.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
}
