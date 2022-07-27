plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "bidon-sdk")
    this.set("AdapterVersionName", "0.0.1")
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:21.1.0")
}