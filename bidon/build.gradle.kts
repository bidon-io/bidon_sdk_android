import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "bidon-sdk")
    this.set("AdapterVersionName", Versions.BidOnVersionName)
}

android {
    defaultConfig {
        ADAPTER_VERSION = Versions.BidOnVersionName
    }
}

dependencies {
    implementation(Dependencies.Google.PlayServicesAds)
    implementation(Dependencies.Ktor.KtorClientCore)
    implementation(Dependencies.Ktor.KtorClientOkHttp)
    implementation(Dependencies.Ktor.KtorClientLogging)
    implementation(Dependencies.Ktor.KtorClientLoggingLongBack)
    implementation(Dependencies.Ktor.KtorClientEncoding)
    implementation(Dependencies.Ktor.KtorClientNegotiation)
    implementation(Dependencies.Ktor.KtorSerializationKotlinxJson)
}
