object Versions {
    const val BidONVersionName = "0.0.3-alpha"

    object Adapters {
        const val Admob = "0.0.1-alpha1"
        const val Applovin = "0.0.1-alpha2"
        const val Appsflyer = "0.0.1-alpha3"
        const val BidMachine = "0.0.1-alpha4"
        const val IronSource = "0.0.1-alpha5"
        const val FairBid = "0.0.1-alpha6"
    }
}

object Dependencies {

    object SdkAdapter {
        const val AppsflyerVersion = "6.7.0"
        const val Appsflyer = "com.appsflyer:af-android-sdk:$AppsflyerVersion"
        const val AppsflyerAdRevenue = "com.appsflyer:adrevenue:6.5.4"
    }

    object Library {
        const val PlayServicesAds = "com.google.android.gms:play-services-ads:21.1.0"

        private const val ktorVersion = "2.1.0"
        const val KtorClientCore = "io.ktor:ktor-client-core:$ktorVersion"
        const val KtorClientOkHttp = "io.ktor:ktor-client-okhttp:$ktorVersion"
        const val KtorClientLogging = "io.ktor:ktor-client-logging:$ktorVersion"
        const val KtorClientEncoding = "io.ktor:ktor-client-encoding:$ktorVersion"
        const val KtorClientNegotiation = "io.ktor:ktor-client-content-negotiation:$ktorVersion"
        const val KtorSerializationKotlinxJson = "io.ktor:ktor-serialization-kotlinx-json:$ktorVersion"

        private const val kotlinxCoroutinesVersion = "1.6.4"
        const val KotlinxCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion"
        const val KotlinxCoroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxCoroutinesVersion"
    }
}