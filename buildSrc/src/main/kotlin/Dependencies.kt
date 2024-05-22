object Dependencies {

    object SdkAdapter {
        const val AppsflyerVersion = "6.9.0"
        const val Appsflyer = "com.appsflyer:af-android-sdk:$AppsflyerVersion"
        const val AppsflyerAdRevenue = "com.appsflyer:adrevenue:$AppsflyerVersion"
    }

    object Kotlin {
        const val kotlinVersion = "1.8.10"

        /**
         * [Compatibility](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
         */
        const val kotlinCompilerExtensionVersion = "1.4.4"
        const val bom = "org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect"

        object Coroutines {
            const val bom = "org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.0"
            const val KotlinxCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core"
            const val KotlinxCoroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android"
        }

    }

    object Android {
        const val gradlePlugin = "com.android.tools.build:gradle:7.4.2"
        const val compileSdkVersion = 33
        const val minSdkVersion = 21

        const val CoreKtx = "androidx.core:core-ktx:1.6.0"
        const val Annotation = "androidx.annotation:annotation:1.1.0"
    }

    object Google {
        const val Services = "com.google.gms:google-services:4.3.14"
        const val PlayServicesAds = "com.google.android.gms:play-services-ads:23.0.0"
        const val PlayServicesAdsIdentifier = "com.google.android.gms:play-services-ads-identifier:18.0.1"
    }
}