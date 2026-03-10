import org.gradle.api.Plugin
import org.gradle.api.Project

class MagicanDependencyLockingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        dependencyLocking {
            lockAllConfigurations()
        }
    }
}
