import org.gradle.api.JavaVersion

plugins {
    kotlin("android")
    id("com.android.library")
    kotlin("plugin.serialization")
}

android {
    compileSdk = Dependencies.Android.compileSdkVersion
    defaultConfig {
        minSdk = Dependencies.Android.minSdkVersion
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile(name = "proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(Dependencies.Kotlin.Coroutines.KotlinxCoroutinesCore)
    implementation(Dependencies.Kotlin.Coroutines.KotlinxCoroutinesAndroid)
    implementation(Dependencies.Kotlin.reflect)
    implementation(Dependencies.Kotlin.Serialization.json)
    implementation(Dependencies.Android.CoreKtx)
    implementation(Dependencies.Android.Annotation)

    /**
     * Testing
     */
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.12.5")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}