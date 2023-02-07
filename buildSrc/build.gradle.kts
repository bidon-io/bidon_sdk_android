plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
    implementation(GradleDependencies.Android.gradlePlugin)
    implementation(GradleDependencies.Kotlin.gradlePlugin)
    implementation(GradleDependencies.Kotlin.Serialization.gradlePlugin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}