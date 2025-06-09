import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "ironsource-adapter")
    this.set("AdapterVersionName", Versions.Adapters.IronSource)
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

    implementation("com.unity3d.ads-mediation:mediation-sdk:8.8.0")
}
