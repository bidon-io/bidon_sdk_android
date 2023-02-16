plugins {
    id("com.android.application")
    kotlin("android")
}
// apply plugin: "applovin-quality-service"
// applovin {
//    apiKey "nEbqcf3QG6eAEo2g-uiJ68vz5F79ESvmug4ylkTR3bptCQ3zLDKdG6gp_CfhqOg9cu6MP5yT88tGDhNhbHXL60"
// }

android {
    compileSdk = 33

    defaultConfig {
        // applicationId "com.bidon.demo"
        applicationId = "com.appodealstack.demo"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("myConfig") {
            storeFile = file("bidon_keystore")
            storePassword = "123456"
            keyAlias = "bidon_alias"
            keyPassword = "123456"
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules-consumer.pro")
            signingConfig = signingConfigs.getByName("myConfig")
        }
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.0"
    }
}

dependencies {
//    implementation("com.applovin:applovin-sdk:11.6.1")
//    implementation("com.appsflyer:af-android-sdk:6.9.4")
//    implementation("com.appsflyer:adrevenue:6.9.1")/

//    implementation("io.bidon:bidon-sdk:0.1.0-Beta")
//    implementation("io.bidon:admob-adapter:0.1.0.1-Beta")
//    implementation("io.bidon:bidmachine-adapter:0.1.0.1-Beta")
//    implementation("io.bidon:applovin-adapter:0.1.0.1-Beta")

    implementation(project(":bidon"))
    implementation(project(":adapter:bidmachine"))
    implementation(project(":adapter:admob"))
    implementation(project(":adapter:applovin"))
//    implementation(project(":adapter:appsflyer"))
//    implementation(project(":adapter:fyber"))
//    implementation(project(":adapter:ironsource"))

    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.google.accompanist:accompanist-permissions:0.29.1-alpha")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.5.5")
    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("androidx.annotation:annotation:1.5.0")

    // compose
    val composeVersion = "1.2.1"
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.material:material-icons-core:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0-alpha06")
    implementation("androidx.navigation:navigation-compose:2.5.1")
    implementation("com.google.accompanist:accompanist-flowlayout:0.23.1")
    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.3.3")
}