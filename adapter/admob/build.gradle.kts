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

// publishInfo {
//    versionName = "0.0.1"
//    artifactId = "admob-adapter"
// }

dependencies {
    implementation(project(":bidon"))
    implementation(Dependencies.Library.PlayServicesAds)
}
