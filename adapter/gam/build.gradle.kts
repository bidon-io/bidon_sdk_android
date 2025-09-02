import ext.ADAPTER_VERSION
import ext.Dependencies
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "gam-adapter"
    versionName = Versions.Adapters.Gam
}

android {
    namespace = "org.bidon.gam"
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Gam
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation(Dependencies.Google.PlayServicesAds)
}
