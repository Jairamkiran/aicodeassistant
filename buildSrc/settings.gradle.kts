/*
 * buildSrc has its own settings so it can consume the SAME version catalog as
 * the main build. Without this block the `libs` accessor is not available to
 * the convention plugins.
 */
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
