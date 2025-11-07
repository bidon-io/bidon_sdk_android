import ext.BIDON_SDK_VERSION
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
        BIDON_SDK_VERSION = Versions.BidonVersionName
    }
}
