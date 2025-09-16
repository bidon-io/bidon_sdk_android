import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "moloco-adapter"
    versionName = Versions.Adapters.Moloco
}

android {
    namespace = "org.bidon.moloco"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Moloco
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.moloco.sdk:moloco-sdk:3.12.0")
}
