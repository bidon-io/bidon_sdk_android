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
}
