import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.configure

class SignaturePlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        val signaturePropertiesExtension = extensions.create(
            "signatureProperties",
            SignaturePropertiesExtension::class.java,
            this
        )
        val androidComponents =
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        androidComponents.finalizeDsl {
            extensions.configure<ApplicationExtension> {
                signingConfigs.create(signConfigName).apply {
                    val storeFilePath =
                        signaturePropertiesExtension.storeFilePath.orNull ?: "debug.keystore"
                    val storePassword =
                        signaturePropertiesExtension.storePassword.orNull ?: "android"
                    val keyAlias = signaturePropertiesExtension.keyAlias.orNull ?: "androiddebugkey"
                    val keyPassword = signaturePropertiesExtension.keyPassword.orNull ?: "android"
                    storeFile = project.file(storeFilePath)
                    this.storePassword = storePassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                }
                buildTypes {
                    release {
                        signingConfig = signingConfigs.named("myConfig").get()
                    }
                }
            }
        }
    }
}

abstract class SignaturePropertiesExtension(project: Project) {
    val storeFilePath: Property<String?> = project.objects.property(String::class.java)
    val storePassword: Property<String?> = project.objects.property(String::class.java)
    val keyAlias: Property<String?> = project.objects.property(String::class.java)
    val keyPassword: Property<String?> = project.objects.property(String::class.java)
}

private const val signConfigName = "myConfig"