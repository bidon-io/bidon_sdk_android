import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "yandex-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Yandex)
}

android {
    namespace = "org.bidon.yandex"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Yandex
    }
}

dependencies {
    compileOnly(project(":bidon"))

    implementation("com.yandex.android:mobileads:5.10.0")
}