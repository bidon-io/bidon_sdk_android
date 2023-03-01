import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "bidon-sdk")
    this.set("AdapterVersionName", Versions.BidonVersionName)
}

android {
    namespace = "org.bidon.sdk"
    defaultConfig {
        ADAPTER_VERSION = Versions.BidonVersionName
    }
}

dependencies {
    implementation(Dependencies.Google.PlayServicesAds)
}
