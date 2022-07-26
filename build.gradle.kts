// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://artifacts.applovin.com/android")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
        classpath("com.google.gms:google-services:4.3.13")
    }
}

val getVersionName = "0.0.1"
val getArtifactId = "bidon"

subprojects {
    apply(plugin = "maven-publish")

    configure<PublishingExtension> {
        val githubProperties = java.util.Properties()
        githubProperties.load(java.io.FileInputStream(rootProject.file("github.properties")))
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/appodeal/Mads-SDK-Android")
                credentials {
                    username = githubProperties["gpr.usr"] as? String ?: System.getenv("GPR_USER")
                    password = githubProperties["gpr.key"] as? String ?: System.getenv("GPR_API_KEY")
                }
            }
        }

        publications {
            register<MavenPublication>("gpr") {
                groupId = "com.appodealstack.bidon" // Replace with group ID
                artifactId = getArtifactId
                version = getVersionName
                artifact("$buildDir/outputs/aar/${getArtifactId}-release.aar")
            }
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}