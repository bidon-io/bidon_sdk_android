plugins {
    id("common")
  //  id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "applovin-decorator")
    this.set("AdapterVersionName", Versions.VersionName)
}

dependencies {
    compileOnly(project(":bidon"))
    implementation("com.applovin:applovin-sdk:11.4.4")
}