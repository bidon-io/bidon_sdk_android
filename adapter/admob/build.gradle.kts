import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "admob-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Admob)
}

android {
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Admob
    }
}

dependencies {
    implementation(project(":bidon"))
    implementation(Dependencies.Google.PlayServicesAds)
}
