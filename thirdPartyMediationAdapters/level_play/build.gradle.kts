import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    groupId = "com.ironsource.adapters"
    artifactId = "bidon-adapter"
    versionName = Versions.ThirdPartyMediationAdapters.LevelPlay
}

android {
    namespace = "com.ironsource.adapters.custom.bidon"
    defaultConfig {
        ADAPTER_VERSION = Versions.ThirdPartyMediationAdapters.LevelPlay
    }
}

dependencies {
    implementation(projects.bidon)

    compileOnly("com.unity3d.ads-mediation:mediation-sdk:8.10.0")
    testImplementation("com.unity3d.ads-mediation:mediation-sdk:8.10.0")
}
