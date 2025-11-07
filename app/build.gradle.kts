import ext.Dependencies
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("sample-app-config")
    id("signature")
    id("compose")
}

val defaultPackage = "org.bidon.demoapp"
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

sampleAppProperties {
    appBundle = keystoreProperties["DEMO_APPLICATION_ID"] as? String
    bidonApiKey = keystoreProperties["BIDON_API_KEY"] as? String
    stagingUsername = keystoreProperties["STAGING_BASIC_AUTH_USERNAME"] as? String
    stagingPassword = keystoreProperties["STAGING_BASIC_AUTH_PASSWORD"] as? String
    admobAppId = keystoreProperties["MOBILE_ADS_APPLICATION_ID"] as? String
}

signatureProperties {
    storePassword = keystoreProperties["storeSigningKey"] as? String
    keyAlias = keystoreProperties["signingKeyAlias"] as? String
    keyPassword = keystoreProperties["signingKeyPassword"] as? String
    storeFilePath = keystoreProperties["storeSigningFileName"] as? String
}

android {
    compileSdk = Dependencies.Android.compileSdkVersion
    namespace = defaultPackage
    defaultConfig {
        minSdk = Dependencies.Android.minSdkVersion
        targetSdk = Dependencies.Android.targetSdkVersion

        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
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
}

dependencies {
    implementation(projects.bidon)
    implementation(projects.adapter.admob)
    implementation(projects.adapter.amazon)
    implementation(projects.adapter.applovin)
    implementation(projects.adapter.bidmachine)
    implementation(projects.adapter.bigoads)
    implementation(projects.adapter.chartboost)
    implementation(projects.adapter.dtexchange)
    implementation(projects.adapter.gam)
    implementation(projects.adapter.inmobi)
    implementation(projects.adapter.ironsource)
    implementation(projects.adapter.meta)
    implementation(projects.adapter.mintegral)
    implementation(projects.adapter.mobilefuse)
    implementation(projects.adapter.moloco)
    implementation(projects.adapter.taurusx)
    implementation(projects.adapter.unityads)
    implementation(projects.adapter.startio)
    implementation(projects.adapter.vkads)
    implementation(projects.adapter.vungle)
    implementation(projects.adapter.yandex)

    implementation(Dependencies.Google.PlayServicesAdsIdentifier)

    implementation("com.chartboost:chartboost-mediation-sdk:4.0.0")
    implementation("com.google.android.gms:play-services-ads:22.5.0")

    implementation("com.google.accompanist:accompanist-permissions:0.29.1-alpha")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation(Dependencies.Android.CoreKtx)
    implementation(Dependencies.Android.Annotation)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // compose
    val composeVersion = "1.9.3"
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")

    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0-alpha07")
    implementation("androidx.navigation:navigation-compose:2.6.0")
    implementation("com.google.accompanist:accompanist-flowlayout:0.23.1")
    // Tests
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
}
