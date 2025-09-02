import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "bigoads-adapter"
    versionName = Versions.Adapters.BigoAds
}

android {
    namespace = "org.bidon.bigoads"
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.BigoAds
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.bigossp:bigo-ads:5.4.0")
}
