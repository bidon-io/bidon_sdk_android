import ext.ADAPTER_VERSION
import ext.Dependencies
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "admob-adapter"
    versionName = Versions.Adapters.Admob
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
