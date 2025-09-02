import ext.ADAPTER_VERSION
import ext.Dependencies
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "dtexchange-adapter"
    versionName = Versions.Adapters.DTExchange
}

android {
    namespace = "org.bidon.dtexchange"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.DTExchange
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.fyber:marketplace-sdk:8.3.8")
    implementation(Dependencies.Google.PlayServicesAdsIdentifier)
}
