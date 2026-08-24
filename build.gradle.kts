/*
 * Root build. Intentionally thin: all shared configuration lives in the
 * buildSrc convention plugins (project-isolation friendly, configuration-cache
 * friendly). The root project itself produces no artifacts.
 */
plugins {
    base
}

tasks.register("printModules") {
    group = "help"
    description = "Lists all Gradle modules and their kind."
    doLast {
        subprojects.forEach { println("${it.path}") }
    }
}
