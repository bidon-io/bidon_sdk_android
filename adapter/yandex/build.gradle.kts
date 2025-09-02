import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "yandex-adapter"
    versionName = Versions.Adapters.Yandex
}

android {
    namespace = "org.bidon.yandex"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Yandex
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.yandex.android:mobileads:7.15.0")
}
