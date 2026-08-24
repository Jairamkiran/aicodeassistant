import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/**
 * Helper to reach the `libs` version catalog from precompiled script plugins,
 * which do not get the generated type-safe `libs` accessor. Centralising it
 * here keeps the convention plugins readable.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalStateException("Missing library alias '$alias' in version catalog") }

internal fun VersionCatalog.ver(alias: String): String =
    findVersion(alias).orElseThrow { IllegalStateException("Missing version alias '$alias' in version catalog") }
        .requiredVersion

/** Formats a catalog library alias as a `group:name:version` coordinate string (for mavenBom). */
internal fun VersionCatalog.coord(alias: String): String {
    val dep = lib(alias).get()
    val v = dep.versionConstraint.requiredVersion
    return "${dep.module.group}:${dep.module.name}" + if (v.isNotBlank()) ":$v" else ""
}
