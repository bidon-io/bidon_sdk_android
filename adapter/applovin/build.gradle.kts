plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "applovin-decorator")
    this.set("AdapterVersionName", "0.0.1")
}

dependencies {
    api(project(":bidon"))
    implementation("com.applovin:applovin-sdk:11.4.4")
}