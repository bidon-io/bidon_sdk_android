package ext

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

object Dependencies {
    object Kotlin {
        const val kotlinVersion = "2.1.0"
        val kotlinTarget = KotlinVersion.KOTLIN_2_1

        /**
         * [Compatibility](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
         */
        const val bom = "org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect"

        object Coroutines {
            const val bom = "org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.0"
            const val KotlinxCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core"
            const val KotlinxCoroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android"
        }

    }

    object Android {
        const val compileSdkVersion = 35
        const val targetSdkVersion = 35
        const val minSdkVersion = 23

        const val CoreKtx = "androidx.core:core-ktx:1.6.0"
        const val Annotation = "androidx.annotation:annotation:1.6.0"
    }

    object Java {
        const val javaVersion = 11
        val javaCompile = JvmTarget.JVM_11
    }

    object Google {
        const val PlayServicesAds = "com.google.android.gms:play-services-ads:24.5.0"
        const val AppSet = "com.google.android.gms:play-services-appset:16.0.0"
        const val PlayServicesAdsIdentifier =
            "com.google.android.gms:play-services-ads-identifier:18.0.1"
    }
}