import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterGroupId", "com.ironsource.adapters")
    this.set("AdapterArtifactId", "bidon-adapter")
    this.set("AdapterVersionName", Versions.ThirdPartyMediationAdapters.LevelPlay)
}

android {
    namespace = "com.ironsource.adapters.custom.bidon"
    defaultConfig {
        ADAPTER_VERSION = Versions.ThirdPartyMediationAdapters.LevelPlay
    }
}

dependencies {
    implementation(projects.bidon)

    compileOnly("com.unity3d.ads-mediation:mediation-sdk:8.8.0")
    testImplementation("com.unity3d.ads-mediation:mediation-sdk:8.8.0")
}
