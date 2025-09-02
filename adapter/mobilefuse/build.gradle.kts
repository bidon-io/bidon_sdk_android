import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "mobilefuse-adapter"
    versionName = Versions.Adapters.MobileFuse
}

android {
    namespace = "org.bidon.mobilefuse"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.MobileFuse
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.mobilefuse.sdk:mobilefuse-sdk-core:1.9.2")
}
