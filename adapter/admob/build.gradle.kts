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
    namespace = "org.bidon.admob"
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Admob
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation(Dependencies.Google.PlayServicesAds)
}
