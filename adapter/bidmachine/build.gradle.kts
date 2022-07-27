plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "bidmachine-adapter")
    this.set("AdapterVersionName", Versions.VersionName)
}

dependencies {
    compileOnly(project(":bidon"))
    implementation("io.bidmachine:ads:1.9.7")
    implementation("io.bidmachine:ads.adapters.admob:1.9.4.22")
}