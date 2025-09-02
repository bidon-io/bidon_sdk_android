import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "ironsource-adapter"
    versionName = Versions.Adapters.IronSource
}

android {
    namespace = "org.bidon.ironsource"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.IronSource
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.unity3d.ads-mediation:mediation-sdk:8.10.0")
}
