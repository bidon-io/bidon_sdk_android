import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "vungle-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Vungle)
}

android {
    namespace = "org.bidon.vungle"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Vungle
    }
}

dependencies {
    compileOnly(project(":bidon"))

    implementation("com.vungle:publisher-sdk-android:6.12.1")
}
