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
    namespace = "org.bidon.bidmachine"
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.BidMachine
    }
}

dependencies {
    compileOnly(project(":bidon"))
    testImplementation(project(":bidon"))

    implementation("io.bidmachine:ads:2.4.0")
}
