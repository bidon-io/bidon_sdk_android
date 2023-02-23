import ext.ADAPTER_VERSION

plugins {
    id("common")
    //  id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "fairbid-decorator")
    this.set("AdapterVersionName", Versions.Adapters.FairBid)
}

android {
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.FairBid
    }
}

dependencies {
    compileOnly(project(":bidon"))
    implementation("com.fyber.omsdk:om-sdk:1.3.28")
    implementation("com.fyber:fairbid-sdk:3.28.1")
    implementation("com.fyber:marketplace-sdk:8.1.3")
}
