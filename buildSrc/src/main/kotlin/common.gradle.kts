import org.gradle.api.JavaVersion

plugins {
    kotlin("android")
    id("com.android.library")
}

android {
    compileSdk = Dependencies.Android.compileSdkVersion
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        minSdk = Dependencies.Android.minSdkVersion
        consumerProguardFiles("proguard-rules-consumer.pro")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(Dependencies.Kotlin.bom))
    implementation(Dependencies.Kotlin.reflect)

    implementation(platform(Dependencies.Kotlin.Coroutines.bom))
    implementation(Dependencies.Kotlin.Coroutines.KotlinxCoroutinesCore)
    implementation(Dependencies.Kotlin.Coroutines.KotlinxCoroutinesAndroid)

    implementation(Dependencies.Android.CoreKtx)
    implementation(Dependencies.Android.Annotation)

    /**
     * Testing
     */
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    testImplementation("io.mockk:mockk:1.13.5") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    testImplementation("com.google.truth:truth:1.1.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20210307")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}