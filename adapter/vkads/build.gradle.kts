import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "vkads-adapter")
    this.set("AdapterVersionName", Versions.Adapters.VkAds)
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

    implementation("com.my.target:mytarget-sdk:5.24.0")
}
