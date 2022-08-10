plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "bidon-sdk")
    this.set("AdapterVersionName", Versions.BidONVersionName)
}

dependencies {
    implementation(Dependencies.Library.PlayServicesAds)
}