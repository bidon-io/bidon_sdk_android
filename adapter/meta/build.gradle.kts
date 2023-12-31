import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "meta-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Meta)
}

android {
    namespace = "org.bidon.meta"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Meta
    }
}

dependencies {
    compileOnly(project(":bidon"))

    implementation("com.facebook.android:audience-network-sdk:6.16.0")
}
