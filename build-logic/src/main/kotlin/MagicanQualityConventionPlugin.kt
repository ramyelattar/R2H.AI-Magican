import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class MagicanQualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")

        extensions.configure(DetektExtension::class.java) {
            buildUponDefaultConfig = true
            allRules = false
            parallel = true
            autoCorrect = false
            ignoreFailures = false
            source.from(
                "src/main/java",
                "src/test/java",
                "src/androidTest/java"
            )
        }

        tasks.withType(Detekt::class.java).configureEach {
            jvmTarget = "17"
            reports {
                xml.required.set(true)
                html.required.set(true)
                sarif.required.set(true)
                md.required.set(false)
            }
        }

        extensions.configure(KtlintExtension::class.java) {
            debug.set(false)
            verbose.set(true)
            outputToConsole.set(true)
            ignoreFailures.set(false)
            filter {
                exclude("**/build/**")
                include("**/*.kt")
                include("**/*.kts")
            }
        }
    }
}
