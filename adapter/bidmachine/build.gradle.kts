plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "bidmachine-adapter")
    this.set("AdapterVersionName", "0.0.1")
}

dependencies {
    compileOnly(project(":bidon"))
    implementation("io.bidmachine:ads:1.9.7")
    implementation("io.bidmachine:ads.adapters.admob:1.9.4.22")
}