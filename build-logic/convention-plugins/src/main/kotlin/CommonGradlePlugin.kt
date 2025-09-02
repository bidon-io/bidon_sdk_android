import com.android.build.gradle.LibraryExtension
import ext.Dependencies
import ext.Dependencies.Java.javaVersion
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.exclude
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class CommonGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        pluginManager.apply {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
            apply("publish-adapter")
        }

        extensions.configure<LibraryExtension> {
            compileSdk = Dependencies.Android.compileSdkVersion
            lint.targetSdk = Dependencies.Android.targetSdkVersion
            testOptions.targetSdk = Dependencies.Android.targetSdkVersion
            defaultConfig {
                minSdk = Dependencies.Android.minSdkVersion
                consumerProguardFiles("proguard-rules-consumer.pro")
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(javaVersion)
                targetCompatibility = JavaVersion.toVersion(javaVersion)
            }

            buildFeatures {
                buildConfig = true
            }

            testOptions {
                unitTests.isReturnDefaultValues = true
            }

            buildTypes {
                debug {
                    isMinifyEnabled = false
                }
                release {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile(name = "proguard-android-optimize.txt"),
                        "proguard-rules-consumer.pro"
                    )
                }
            }
            flavorDimensions += "server"
            productFlavors {
                create("production") {
                    description = "Production backend"
                    dimension = "server"
                }
                create("serverless") {
                    description = "No /config and /auction/* requests. Set it manually for tests"
                    dimension = "server"
                }
            }
        }

        project.tasks.withType(KotlinJvmCompile::class.java).configureEach {
            compilerOptions {
                jvmTarget.set(Dependencies.Java.javaCompile)
                languageVersion.set(Dependencies.Kotlin.kotlinTarget)
                freeCompilerArgs.addAll(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=kotlinx.coroutines.InternalCoroutinesApi",
                    "-opt-in=kotlinx.coroutines.FlowPreview"
                )
            }
        }

        dependencies {
            add("implementation", platform(Dependencies.Kotlin.bom))
            add("implementation", Dependencies.Kotlin.reflect)
            add("implementation", platform(Dependencies.Kotlin.Coroutines.bom))
            add("implementation", Dependencies.Kotlin.Coroutines.KotlinxCoroutinesCore)
            add("implementation", Dependencies.Kotlin.Coroutines.KotlinxCoroutinesAndroid)
            add("implementation", Dependencies.Android.CoreKtx)
            add("implementation", Dependencies.Android.Annotation)

            /**
             * Testing
             */
            add("testImplementation", "junit:junit:4.13.2")
            add("testImplementation", "org.jetbrains.kotlin:kotlin-test:${Dependencies.Kotlin.kotlinVersion}")
            add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit:${Dependencies.Kotlin.kotlinVersion}")
            add("testImplementation", "org.jetbrains.kotlinx:kotlinx-coroutines-test")
            add("testImplementation", dependencies.create("io.mockk:mockk:1.13.5").apply {
                (this as ExternalModuleDependency).exclude("org.slf4j", "slf4j-api")
            })
            add("testImplementation", "com.google.truth:truth:1.1.4")
            add("testImplementation", "org.json:json:20210307")
            add("androidTestImplementation", "androidx.test.ext:junit:1.1.5")
            add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.5.1")
        }
    }
}
