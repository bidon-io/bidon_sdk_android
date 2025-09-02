import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "vungle-adapter"
    versionName = Versions.Adapters.Vungle
}

android {
    namespace = "org.bidon.vungle"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Vungle
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.vungle:vungle-ads:7.5.0")
}
