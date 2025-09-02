import ext.Dependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class CoreGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        pluginManager.apply {
            apply("common")
        }

        dependencies {
            add("implementation", Dependencies.Google.AppSet)
        }
    }
}
