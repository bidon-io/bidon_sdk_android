import ext.BIDON_API_KEY
import org.jetbrains.kotlin.konan.properties.hasProperty
import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
}

val defaultPackage = "org.bidon.demoapp"
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    compileSdk = 33
    namespace = defaultPackage

    defaultConfig {
        applicationId = keystoreProperties["DEMO_APPLICATION_ID"] as? String ?: defaultPackage
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MOBILE_ADS_APPLICATION_ID"] = keystoreProperties["MOBILE_ADS_APPLICATION_ID"] as? String ?: ""
        BIDON_API_KEY = keystoreProperties["BIDON_API_KEY"] as? String ?: ""
    }
    signingConfigs {
        create("myConfig") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeSigningFileName"] as String)
                storePassword = keystoreProperties["storeSigningKey"] as String
                keyAlias = keystoreProperties["signingKeyAlias"] as String
                keyPassword = keystoreProperties["signingKeyPassword"] as String
            }
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
    flavorDimensions += "server"
    productFlavors {
        create("production") {
            description = "Production backend x.appbaqend.com"
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.0"
    }
}

dependencies {
//    implementation("io.bidon:bidon-sdk:0.1.0-Beta")
//    implementation("io.bidon:admob-adapter:0.1.0.1-Beta")
//    implementation("io.bidon:bidmachine-adapter:0.1.0.1-Beta")
//    implementation("io.bidon:applovin-adapter:0.1.0.1-Beta")
//    implementation("io.bidon:dtexchange-adapter:0.1.0.1-Beta")
//    implementation("io.bidon:unityads-adapter:0.1.0.1-Beta")

    implementation(project(":bidon"))
    implementation(project(":adapter:bidmachine"))
    implementation(project(":adapter:admob"))
    implementation(project(":adapter:applovin"))
    implementation(project(":adapter:dtexchange"))
    implementation(project(":adapter:unityads"))
//    implementation(project(":adapter:appsflyer"))
//    implementation(project(":adapter:fyber"))
//    implementation(project(":adapter:ironsource"))

    implementation(Dependencies.Google.PlayServicesAdsIdentifier)

    implementation("com.google.accompanist:accompanist-permissions:0.29.1-alpha")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.5.5")
    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("androidx.annotation:annotation:1.6.0")

    // compose
    val composeVersion = "1.3.1"
    val material3Version = "1.0.1"
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")

    implementation("androidx.compose.material3:material3:$material3Version")
    implementation("androidx.compose.material:material-icons-core:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0-alpha07")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("com.google.accompanist:accompanist-flowlayout:0.23.1")
    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
}