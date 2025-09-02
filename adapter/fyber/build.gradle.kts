import ext.ADAPTER_VERSION
import ext.Versions
import ext.Dependencies

plugins {
    id("common")
    id("publish-adapter")
}

publishAdapter {
    artifactId = "fyber-adapter"
    versionName = Versions.Adapters.Fyber
}

android {
    namespace = "org.bidon.fyber"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Fyber
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    val fyberMarketplaceVersion = "8.2.1"

    implementation("com.fyber.omsdk:om-sdk:1.3.30")
    implementation("com.fyber:fairbid-sdk:3.28.1")
    implementation("com.fyber:marketplace-sdk:$fyberMarketplaceVersion")
    implementation(Dependencies.Google.PlayServicesAdsIdentifier)
}
