import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

plugins {
    java
}

allprojects {
    group = "org.pangya"
    version = "0.1.0-SNAPSHOT"
}

/**
 * Test tasks share one external resource: the Compose Postgres database (`pangya`). Under
 * `org.gradle.parallel=true` several modules' integration tests would migrate/seed/mutate that
 * single database concurrently, so counter- and idempotency-based assertions collide and fail
 * non-deterministically (e.g. FlywayMigrationTest, GameFlowIT attendance/achievement counters).
 * Declaring this build service with maxParallelUsages=1 and wiring every Test task to it makes
 * Gradle run the test tasks one at a time while keeping the rest of the build parallel, so
 * `./gradlew test` is deterministic without forcing `--max-workers=1`.
 */
abstract class SharedDatabaseService : BuildService<BuildServiceParameters.None>

val sharedDatabase =
    gradle.sharedServices.registerIfAbsent("pangyaSharedDatabase", SharedDatabaseService::class.java) {
        maxParallelUsages.set(1)
    }

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        workingDir = rootProject.projectDir
        // Serialize test tasks: they all share the single Compose Postgres database.
        usesService(sharedDatabase)
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
