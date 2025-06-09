import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterGroupId", "com.applovin.mediation")
    this.set("AdapterArtifactId", "bidon-adapter")
    this.set("AdapterVersionName", Versions.ThirdPartyMediationAdapters.ApplovinMax)
}

android {
    namespace = "com.applovin.mediation.adapters"
    defaultConfig {
        ADAPTER_VERSION = Versions.ThirdPartyMediationAdapters.ApplovinMax
    }
}

dependencies {
    implementation(projects.bidon)

    compileOnly("com.applovin:applovin-sdk:13.2.0")
    testImplementation("com.applovin:applovin-sdk:13.2.0")
}
