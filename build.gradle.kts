import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.testing.Test
import java.io.File

plugins {
    id("com.r2h.magican.quality") apply false
    id("com.r2h.magican.dependency-locking") apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.cyclonedx.bom)
    alias(libs.plugins.owasp.dependency.check)
}

tasks.register("enforceArchitectureBoundaries") {
    group = "verification"
    description = "Fails build on forbidden module dependency edges."
    notCompatibleWithConfigurationCache("Inspects full project graph at execution time.")

    doLast {
        val violations = linkedSetOf<String>()

        subprojects.forEach { project ->
            project.configurations.forEach { config ->
                config.dependencies
                    .withType(ProjectDependency::class.java)
                    .forEach dependencyLoop@{ dependency ->
                        val from = project.path
                        val to = dependency.dependencyProject.path
                        if (from == to) return@dependencyLoop

                        val featureToFeature = from.startsWith(":features:") && to.startsWith(":features:")
                        val coreToFeature = from.startsWith(":core:") && to.startsWith(":features:")
                        val aiToFeature = from.startsWith(":ai:") && to.startsWith(":features:")
                        val aiToApp = from.startsWith(":ai:") && to == ":app"

                        if (featureToFeature || coreToFeature || aiToFeature || aiToApp) {
                            violations += "$from -> $to (via ${config.name})"
                        }
                    }
            }
        }

        if (violations.isNotEmpty()) {
            val body = violations.joinToString(separator = "\n - ", prefix = "\n - ")
            throw GradleException("Forbidden module dependencies detected:$body")
        }
    }
}

tasks.register("qualityGate") {
    group = "verification"
    description = "Runs repository-wide policy checks."
    dependsOn("enforceArchitectureBoundaries", "dependencyCheckAnalyze")
}

tasks.register("orchestratorGate") {
    group = "verification"
    description = "Runs compile + debug unit tests for :ai:orchestrator."
    dependsOn(
        ":ai:orchestrator:compileDebugUnitTestKotlin",
        ":ai:orchestrator:testDebugUnitTest"
    )
}

configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    failBuildOnCVSS = 9.0f
    formats = listOf("HTML", "JSON")
    scanConfigurations = listOf("runtimeClasspath", "releaseRuntimeClasspath")
    analyzers.assemblyEnabled = false
}

subprojects {
    if (name != "build-logic") {
        pluginManager.apply("com.r2h.magican.quality")
        pluginManager.apply("com.r2h.magican.dependency-locking")
    }

    tasks.withType<Test>().configureEach {
        val cleanPath = System.getenv("PATH").orEmpty()
            .split(File.pathSeparator)
            .asSequence()
            .map { it.trim().trim('"') }
            .filter { it.isNotBlank() }
            .filterNot { it.contains("Opera GX", ignoreCase = true) }
            .distinct()
            .joinToString(File.pathSeparator)

        environment("PATH", cleanPath)

        val jniDirs = listOf(
            project.layout.projectDirectory.dir("src/test/jniLibs").asFile.absolutePath,
            project.layout.projectDirectory.dir("src/testDebug/jniLibs").asFile.absolutePath
        ).filter { File(it).exists() }

        val cleanLibPath = jniDirs.joinToString(File.pathSeparator)
        if (cleanLibPath.isNotBlank()) {
            jvmArgs("-Djava.library.path=$cleanLibPath")
        }
    }
}

tasks.named("dependencyCheckAnalyze") {
    dependsOn("enforceArchitectureBoundaries")
}

tasks.named("cyclonedxBom") {
    dependsOn("enforceArchitectureBoundaries")
}
