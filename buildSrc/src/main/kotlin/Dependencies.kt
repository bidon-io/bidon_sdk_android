object Dependencies {

    object SdkAdapter {
        const val AppsflyerVersion = "6.9.0"
        const val Appsflyer = "com.appsflyer:af-android-sdk:$AppsflyerVersion"
        const val AppsflyerAdRevenue = "com.appsflyer:adrevenue:$AppsflyerVersion"
    }

    object Kotlin {
        const val kotlinVersion = "1.8.0"

        /**
         * [Compatibility](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
         */
        const val kotlinCompilerExtensionVersion = "1.4.1"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"

        object Coroutines {
            private const val coroutinesVersion = "1.6.4"
            const val KotlinxCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
            const val KotlinxCoroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
        }

    }

    object Android {
        const val gradlePlugin = "com.android.tools.build:gradle:7.4.0"
        const val compileSdkVersion = 33
        const val minSdkVersion = 21

        const val CoreKtx = "androidx.core:core-ktx:1.9.0"
        const val Annotation = "androidx.annotation:annotation:1.4.0"
    }

    object Google {
        const val Services = "com.google.gms:google-services:4.3.15"
        const val PlayServicesAds = "com.google.android.gms:play-services-ads:21.5.0"
        const val PlayServicesAdsIdentifier = "com.google.android.gms:play-services-ads-identifier:18.0.1"
    }

    object Accompanist {
        private const val accompanist = "0.29.1-alpha"
        const val permissions = "com.google.accompanist:accompanist-permissions:$accompanist"
    }
}