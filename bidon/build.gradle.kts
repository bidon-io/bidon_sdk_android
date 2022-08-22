import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "bidon-sdk")
    this.set("AdapterVersionName", Versions.BidONVersionName)
}

android {
    defaultConfig {
        ADAPTER_VERSION = Versions.BidONVersionName
    }
}

dependencies {
    implementation(Dependencies.Library.PlayServicesAds)
    implementation(Dependencies.Library.KtorClientCore)
    implementation(Dependencies.Library.KtorClientOkHttp)
    implementation(Dependencies.Library.KtorClientLogging)
    implementation(Dependencies.Library.KtorClientLoggingLongBack)
    implementation(Dependencies.Library.KtorClientEncoding)
    implementation(Dependencies.Library.KtorClientNegotiation)
    implementation(Dependencies.Library.KtorSerializationKotlinxJson)
}
