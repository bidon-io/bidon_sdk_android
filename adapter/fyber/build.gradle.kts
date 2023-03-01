import ext.ADAPTER_VERSION

plugins {
    id("common")
    //  id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "fyber-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Fyber)
}

android {
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Fyber
    }
}

dependencies {
    compileOnly(project(":bidon"))

    val fyberMarketplaceVersion = "8.2.1"

    implementation("com.fyber.omsdk:om-sdk:1.3.30")
    implementation("com.fyber:fairbid-sdk:3.28.1")
    implementation("com.fyber:marketplace-sdk:$fyberMarketplaceVersion")
    implementation(Dependencies.Google.PlayServicesAdsIdentifier)
}
