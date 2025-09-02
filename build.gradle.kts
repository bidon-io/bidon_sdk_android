buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("0.48.2")
        debug.set(true)
        verbose.set(true)
        android.set(false)
        outputToConsole.set(true)
    }
}

plugins {
    // --- Android plugins ---
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("com.android.test") version "8.7.3" apply false

    // --- Kotlin plugins ---
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false

    // --- Tools & utilities ---
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" apply false
    id("com.google.gms.google-services") version "4.3.14" apply false
}
