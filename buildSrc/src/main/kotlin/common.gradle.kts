plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    compileSdk = 33
    defaultConfig {
        minSdk = 21
        targetSdk = 33
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
        jvmTarget = "1.8"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(Dependencies.Library.KotlinxCoroutinesCore)
    implementation(Dependencies.Library.KotlinxCoroutinesAndroid)

    implementation("androidx.core:core-ktx:1.8.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0-RC")
    implementation("androidx.annotation:annotation:1.4.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.10")

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