import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "mintegral-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Mintegral)
}

android {
    namespace = "org.bidon.mintegral"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Mintegral
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.mbridge.msdk.oversea:mbridge_android_sdk:16.8.81")
}
