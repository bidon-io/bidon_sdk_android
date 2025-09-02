import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "unityads-adapter"
    versionName = Versions.Adapters.UnityAds
}

android {
    namespace = "org.bidon.unityads"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.UnityAds
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.unity3d.ads:unity-ads:4.16.0")
}
