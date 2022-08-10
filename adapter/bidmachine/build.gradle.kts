import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "bidmachine-adapter")
    this.set("AdapterVersionName", Versions.Adapters.BidMachine)
}

android {
    defaultConfig{
        ADAPTER_VERSION = Versions.Adapters.BidMachine
    }
}

dependencies {
    implementation(project(":bidon"))
    implementation("io.bidmachine:ads:1.9.7")
    implementation("io.bidmachine:ads.adapters.admob:1.9.4.22")
}