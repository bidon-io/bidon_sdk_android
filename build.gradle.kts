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
        classpath("com.android.tools.build:gradle:7.2.2")
        val kotlinVersion = "1.7.10"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
        classpath("com.google.gms:google-services:4.3.13")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}