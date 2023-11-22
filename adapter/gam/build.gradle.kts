import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "gam-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Gam)
}

android {
    namespace = "org.bidon.gam"
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Gam
    }
}

dependencies {
    compileOnly(project(":bidon"))
    implementation(Dependencies.Google.PlayServicesAds)
}