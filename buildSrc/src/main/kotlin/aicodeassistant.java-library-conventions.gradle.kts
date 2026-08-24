/*
 * Base convention applied to EVERY module (shared kernel + bounded contexts +
 * deployables). Provides:
 *   - Java 21 toolchain (auto-provisioned if the JDK is absent)
 *   - Spring dependency-management BOM alignment (Boot + Modulith + Resilience4j)
 *   - JUnit 5 / AssertJ test wiring, with a separate `integrationTest` source set
 *     that is EXCLUDED from `./gradlew test` and only runs under `integrationTest`
 *     (so unit builds stay green without Docker; Testcontainers ITs opt in)
 *   - Spotless (google-java-format) + Checkstyle for consistent style
 *   - MapStruct annotation processing (used across mapping-heavy modules)
 */
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
    checkstyle
    pmd
    jacoco
}

group = "com.jairam.aicodeassistant"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

// Repositories are declared centrally in settings.gradle.kts (PREFER_SETTINGS).

// --- BOM alignment: leaf modules declare deps without versions ---------------
dependencyManagement {
    imports {
        mavenBom(libs.coord("spring-boot-bom"))
        mavenBom(libs.coord("spring-modulith-bom"))
        mavenBom(libs.coord("resilience4j-bom"))
    }
}

// --- Dedicated integration-test source set -----------------------------------
val integrationTest: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

configurations["integrationTestImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    testImplementation(libs.lib("spring-boot-starter-test").get())
    testImplementation(libs.lib("assertj").get())
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing", "-Werror"))
}

// Reproducible builds: stable archive contents regardless of build host/time, so
// the same source produces byte-identical jars (auditable, cache-friendly).
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs Testcontainers-backed integration tests (requires a Docker daemon)."
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = integrationTest.output.classesDirs
    classpath = configurations["integrationTestRuntimeClasspath"] + integrationTest.output +
        sourceSets.main.get().output
    shouldRunAfter(tasks.named("test"))
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

// `check` runs unit tests + style, but NOT integrationTest (Docker opt-in).
tasks.named("check") {
    dependsOn(tasks.named("test"))
}

// --- Style: Spotless (google-java-format) ------------------------------------
spotless {
    java {
        googleJavaFormat("1.22.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        target("src/**/*.java")
    }
}

// --- Checkstyle --------------------------------------------------------------
checkstyle {
    toolVersion = libs.ver("checkstyle")
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

// --- PMD ---------------------------------------------------------------------
// Static analysis for real defect categories (error-prone, best-practices) with
// a focused ruleset — style/formatting is already owned by Spotless + Checkstyle,
// so PMD deliberately does not duplicate them. Runs on main sources only.
pmd {
    toolVersion = libs.ver("pmd")
    isConsoleOutput = true
    isIgnoreFailures = false
    ruleSetConfig = resources.text.fromFile(rootProject.file("config/pmd/ruleset.xml"))
    ruleSets = emptyList() // use only our ruleset, not PMD's defaults
}
// PMD on test sources (unit + integration) adds noise without protecting
// production behaviour; run it on main sources only.
tasks.withType<Pmd>().configureEach {
    if (name != "pmdMain") {
        isEnabled = false
    }
}

// --- Coverage ----------------------------------------------------------------
tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
