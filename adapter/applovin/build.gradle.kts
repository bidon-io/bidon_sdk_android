import ext.ADAPTER_SDK_VERSION
import ext.ADAPTER_VERSION

plugins {
    id("common")
  //  id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "applovin-decorator")
    this.set("AdapterVersionName", Versions.Adapters.Applovin)
}

android {
    defaultConfig{
        ADAPTER_VERSION = Versions.Adapters.Applovin
    }
}

dependencies {
    implementation(project(":bidon"))
    implementation("com.applovin:applovin-sdk:11.4.4")
}