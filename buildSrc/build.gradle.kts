plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    implementation("com.android.tools.build:gradle:7.2.1")
    implementation("com.android.tools.build:gradle-api:7.2.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}