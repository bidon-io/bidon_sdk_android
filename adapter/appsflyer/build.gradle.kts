plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "appsflyer-adapter")
    this.set("AdapterVersionName", "0.0.1")
}

dependencies {
    compileOnly(project(":bidon"))
    implementation("com.appsflyer:af-android-sdk:6.7.0")
    implementation("com.appsflyer:adrevenue:6.5.4")
}