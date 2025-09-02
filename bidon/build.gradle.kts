import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("core")
}

publishAdapter {
    artifactId = "bidon-sdk"
    versionName = Versions.BidonVersionName
}

android {
    namespace = "org.bidon.sdk"
    defaultConfig {
        ADAPTER_VERSION = Versions.BidonVersionName
    }
}
