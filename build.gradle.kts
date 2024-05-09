// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(Dependencies.Android.gradlePlugin)
        classpath(Dependencies.Kotlin.gradlePlugin)
        classpath(Dependencies.Google.Services)
    }
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("org.jetbrains.dokka") version "1.8.10"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint") // Version should be inherited from parent
    apply(plugin = "org.jetbrains.dokka")

    // Optionally configure plugin
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        debug.set(true)
        additionalEditorconfigFile.set(file(".editorconfig"))
        disabledRules.set(setOf("final-newline", "no-wildcard-imports", "max-line-length"))
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
