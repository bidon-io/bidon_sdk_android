import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "vkads-adapter"
    versionName = Versions.Adapters.VkAds
}

android {
    namespace = "org.bidon.vkads"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.VkAds
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.my.target:mytarget-sdk:5.27.2")
}
