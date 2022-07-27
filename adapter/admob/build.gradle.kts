plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "admob-adapter")
    this.set("AdapterVersionName", Versions.VersionName)
}

//publishInfo {
//    versionName = "0.0.1"
//    artifactId = "admob-adapter"
//}

dependencies {
    compileOnly(project(":bidon"))
    implementation("com.google.android.gms:play-services-ads:21.1.0")
}