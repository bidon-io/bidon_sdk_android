import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import ext.BIDON_API_KEY
import ext.Dependencies.Java.javaCompile
import ext.Dependencies.Java.javaVersion
import ext.STAGING_BASIC_AUTH_PASSWORD
import ext.STAGING_BASIC_AUTH_USERNAME
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

const val defaultPackage = "org.bidon.demoapp"

class SampleAppConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        val appPropertiesExtension = extensions.create(
            "sampleAppProperties",
            SampleAppPropertiesExtension::class.java,
            this
        )

        pluginManager.apply {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
        }

        val androidComponents =
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidComponents.onVariants { variant ->
            project.logger.lifecycle("variant: ${variant.name}")
            val bundleId = appPropertiesExtension.appBundle.getOrElse(defaultPackage)
            val apiKey = appPropertiesExtension.bidonApiKey.getOrElse("")
            val admobAppId = appPropertiesExtension.admobAppId.getOrElse("")
            val user = appPropertiesExtension.stagingUsername.getOrElse("username")
            val pass = appPropertiesExtension.stagingPassword.getOrElse("password")

            variant.applicationId.set(bundleId)
            variant.BIDON_API_KEY = apiKey
            variant.STAGING_BASIC_AUTH_USERNAME = user
            variant.STAGING_BASIC_AUTH_PASSWORD = pass
            variant.manifestPlaceholders.set(mapOf("MOBILE_ADS_APPLICATION_ID" to admobAppId))
        }
        extensions.configure<ApplicationExtension> {
            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(javaVersion)
                targetCompatibility = JavaVersion.toVersion(javaVersion)
            }
            lint {
                checkReleaseBuilds = false
            }
            buildFeatures {
                buildConfig = true
            }
            packaging {
                resources.excludes.addAll(
                    listOf(
                        "META-INF/LICENSE.txt",
                        "META-INF/NOTICE.txt",
                        "META-INF/*.kotlin_module"
                    )
                )
            }
        }
        project.tasks.withType(KotlinJvmCompile::class.java).configureEach {
            compilerOptions {
                jvmTarget.set(javaCompile)
            }
        }
    }
}

abstract class SampleAppPropertiesExtension(project: Project) {
    val appBundle: Property<String?> = project.objects.property(String::class.java)
    val bidonApiKey: Property<String?> = project.objects.property(String::class.java)
    val stagingUsername: Property<String?> = project.objects.property(String::class.java)
    val stagingPassword: Property<String?> = project.objects.property(String::class.java)
    val admobAppId: Property<String?> = project.objects.property(String::class.java)
}