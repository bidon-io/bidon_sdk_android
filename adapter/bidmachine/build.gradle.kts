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
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.BidMachine
    }
}

dependencies {
    implementation(project(":bidon"))
    implementation("io.bidmachine:ads:1.9.7")
    implementation("io.bidmachine:ads.adapters.admanager:1.9.4.6")

//    implementation("io.bidmachine:ads.adapters.admob:1.9.4.22")

//    implementation("io.bidmachine:ads.networks.criteo:1.9.7.10")
//    implementation("io.bidmachine:ads.networks.pangle:1.9.7.3")
//    implementation("io.bidmachine:ads.networks.amazon:1.9.7.6")
//    implementation("io.bidmachine:ads.networks.adcolony:1.9.7.10")
//    implementation("io.bidmachine:ads.networks.meta_audience:1.9.7.12")
//    implementation("io.bidmachine:ads.networks.my_target:1.9.7.8")
//    implementation("io.bidmachine:ads.networks.vungle:1.9.7.1")
//    implementation("io.bidmachine:ads.networks.tapjoy:1.9.7.8")
//    implementation("io.bidmachine:ads.networks.notsy:1.9.7.1")

    implementation(Dependencies.Library.PlayServicesAds)
}