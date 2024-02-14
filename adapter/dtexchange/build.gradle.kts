import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "dtexchange-adapter")
    this.set("AdapterVersionName", Versions.Adapters.DTExchange)
}

android {
    namespace = "org.bidon.dtexchange"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.DTExchange
    }
}

dependencies {
    compileOnly(project(":bidon"))

    implementation("com.fyber:marketplace-sdk:8.2.5")
    implementation(Dependencies.Google.PlayServicesAdsIdentifier)
}
