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
    val kotlinVersion = "1.7.10"
    implementation("com.android.tools.build:gradle:7.2.2")
    implementation("com.android.tools.build:gradle-api:7.2.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation(kotlin("serialization"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}