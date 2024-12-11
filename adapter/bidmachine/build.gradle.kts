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
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("io.bidmachine:ads:3.1.1")
}
