object Dependencies {

    object SdkAdapter {
        const val AppsflyerVersion = "6.9.0"
        const val Appsflyer = "com.appsflyer:af-android-sdk:$AppsflyerVersion"
        const val AppsflyerAdRevenue = "com.appsflyer:adrevenue:$AppsflyerVersion"
    }

    object Kotlin {
        const val kotlinVersion = "2.1.0"

        /**
         * [Compatibility](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
         */
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
        const val gradlePlugin = "com.android.tools.build:gradle:8.8.2"
        const val compileSdkVersion = 34
        const val minSdkVersion = 23

        const val CoreKtx = "androidx.core:core-ktx:1.6.0"
        const val Annotation = "androidx.annotation:annotation:1.6.0"
    }

    object Google {
        const val Services = "com.google.gms:google-services:4.3.14"
        const val PlayServicesAds = "com.google.android.gms:play-services-ads:24.3.0"
        const val AppSet = "com.google.android.gms:play-services-appset:16.0.0"
        const val PlayServicesAdsIdentifier = "com.google.android.gms:play-services-ads-identifier:18.0.1"
    }
}