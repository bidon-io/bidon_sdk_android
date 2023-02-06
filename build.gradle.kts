// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://artifacts.applovin.com/android")
        maven {
            name = "BidMachine Ads maven repository"
            url = uri("https://artifactory.bidmachine.io/bidmachine")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.1")
        val kotlinVersion = "1.8.10"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
        classpath("com.google.gms:google-services:4.3.15")
    }
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint") // Version should be inherited from parent

    repositories {
        // Required to download KtLint
        mavenCentral()
    }

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
