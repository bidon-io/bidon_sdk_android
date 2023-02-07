object Dependencies {

    object SdkAdapter {
        const val AppsflyerVersion = "6.9.0"
        const val Appsflyer = "com.appsflyer:af-android-sdk:$AppsflyerVersion"
        const val AppsflyerAdRevenue = "com.appsflyer:adrevenue:$AppsflyerVersion"
    }

    object Kotlin {
        const val kotlinVersion = "1.8.0"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"

        object Serialization {
            const val gradlePlugin = "org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion"

            private const val version = "1.4.1"
            const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-core:$version"
            const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:$version"
        }

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
    }

    object Ktor {
        private const val ktorVersion = "2.2.3"
        const val KtorClientCore = "io.ktor:ktor-client-core:$ktorVersion"
        const val KtorClientOkHttp = "io.ktor:ktor-client-okhttp:$ktorVersion"
        const val KtorClientLogging = "io.ktor:ktor-client-logging:$ktorVersion"
        const val KtorClientLoggingLongBack = "ch.qos.logback:logback-classic:1.3.0-beta0"
        const val KtorClientEncoding = "io.ktor:ktor-client-encoding:$ktorVersion"
        const val KtorClientNegotiation = "io.ktor:ktor-client-content-negotiation:$ktorVersion"
        const val KtorSerializationKotlinxJson = "io.ktor:ktor-serialization-kotlinx-json:$ktorVersion"
    }

    object Accompanist {
        private const val accompanist = "0.29.1-alpha"
        const val permissions = "com.google.accompanist:accompanist-permissions:$accompanist"
    }
}