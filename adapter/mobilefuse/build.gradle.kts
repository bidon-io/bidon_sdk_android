import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "mobilefuse-adapter")
    this.set("AdapterVersionName", Versions.Adapters.MobileFuse)
}

android {
    namespace = "org.bidon.mobilefuse"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.MobileFuse
    }
}

dependencies {
    compileOnly(project(":bidon"))
    testImplementation(project(":bidon"))

    implementation("com.mobilefuse.sdk:mobilefuse-sdk-core:1.6.2")
}
