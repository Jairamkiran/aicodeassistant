/*
 * AI Software Engineering Assistant — AI Software Engineering Assistant
 * Root Gradle settings.
 *
 * Topology (see ARCHITECTURE.md):
 *   platform            — shared kernel (java-library, no Spring Boot app)
 *   <bounded contexts>  — one java-library per DDD bounded context
 *   app                 — Spring Boot deployable: the modular monolith (web + API + WS)
 *   indexer-worker      — Spring Boot deployable: async indexing pipeline consumer
 *
 * Bounded contexts are separate Gradle modules so the dependency direction is
 * enforced by the compiler; Spring Modulith adds runtime package-boundary
 * verification on top (see app/src/test ModularityTests).
 */
rootProject.name = "aicodeassistant"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
    }
    // The version catalog is auto-detected at gradle/libs.versions.toml.
}

// --- Shared kernel ------------------------------------------------------------
// platform      : servlet-free core (safe for the non-web worker)
// platform-web  : web adapter kernel (RFC-9457 handler, correlation filter);
//                 only web deployables depend on it, keeping the worker lean.
include(":platform")
include(":platform-web")

// --- Bounded contexts (DDD modules) ------------------------------------------
include(":iam")
include(":repository")
include(":indexing")
include(":retrieval")
include(":conversation")
include(":ai")
include(":codeintel")
include(":integration")
include(":notification")
include(":analytics")
include(":audit")
include(":sdk")

// --- Deployables --------------------------------------------------------------
include(":app")
include(":indexer-worker")
