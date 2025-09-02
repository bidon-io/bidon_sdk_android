import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "mintegral-adapter"
    versionName = Versions.Adapters.Mintegral
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

    implementation("com.mbridge.msdk.oversea:mbridge_android_sdk:16.9.91")
}
