import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "unityads-adapter")
    this.set("AdapterVersionName", Versions.Adapters.UnityAds)
}

android {
    namespace = "org.bidon.unityads"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.UnityAds
    }
}

dependencies {
    compileOnly(project(":bidon"))

    implementation("com.unity3d.ads:unity-ads:4.8.0")
}
