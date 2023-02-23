import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "applovin-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Applovin)
}

android {
    namespace = "org.bidon.applovin"
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Applovin
    }
}

dependencies {
    compileOnly(project(":bidon"))
    implementation("com.applovin:applovin-sdk:11.6.1")
}
