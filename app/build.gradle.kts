plugins {
    id("com.android.application")
    kotlin("android")
}
// apply plugin: "applovin-quality-service"
// applovin {
//    apiKey "nEbqcf3QG6eAEo2g-uiJ68vz5F79ESvmug4ylkTR3bptCQ3zLDKdG6gp_CfhqOg9cu6MP5yT88tGDhNhbHXL60"
// }

android {
    compileSdk = 32

    defaultConfig {
        // applicationId "com.bidon.demo"
        applicationId = "com.appodealstack.demo"
        minSdk = 21
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.0-rc02"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.applovin:applovin-sdk:11.4.4")
    implementation("com.appsflyer:af-android-sdk:6.7.0")
    implementation("com.appsflyer:adrevenue:6.5.4")

    implementation(project(":bidon"))
    implementation(project(":adapter:appsflyer"))
    implementation(project(":adapter:applovin"))
    implementation(project(":adapter:bidmachine"))
    implementation(project(":adapter:admob"))

    implementation(Dependencies.Accompanist.permissions)

    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.5.2")
    implementation("androidx.activity:activity-ktx:1.5.1")
    implementation("androidx.annotation:annotation:1.4.0")

    // compose
    implementation("androidx.compose.runtime:runtime:1.2.1")
    implementation("androidx.compose.ui:ui:1.2.1")
    implementation("androidx.compose.ui:ui-tooling:1.2.1")
    implementation("androidx.compose.foundation:foundation:1.2.1")
    implementation("androidx.compose.material:material:1.2.1")
    implementation("androidx.compose.material:material-icons-core:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.2.1")
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0-alpha03")
    implementation("androidx.navigation:navigation-compose:2.5.1")
    implementation("com.google.accompanist:accompanist-flowlayout:0.23.1")
    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.2.1")
}