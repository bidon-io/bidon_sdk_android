import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    groupId = "com.applovin.mediation"
    artifactId = "bidon-adapter"
    versionName = Versions.ThirdPartyMediationAdapters.ApplovinMax
}

android {
    namespace = "com.applovin.mediation.adapters"
    defaultConfig {
        ADAPTER_VERSION = Versions.ThirdPartyMediationAdapters.ApplovinMax
    }
}

dependencies {
    implementation(projects.bidon)

    compileOnly("com.applovin:applovin-sdk:13.3.1")
    testImplementation("com.applovin:applovin-sdk:13.3.1")
}
