plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "ironsource-decorator")
    this.set("AdapterVersionName", "0.0.1")
}

dependencies {
    api(project(":bidon"))
    implementation("com.ironsource.sdk:mediationsdk:7.2.3.1")
}