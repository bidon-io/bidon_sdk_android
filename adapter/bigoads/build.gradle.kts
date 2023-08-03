import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "bigoads-adapter")
    this.set("AdapterVersionName", Versions.Adapters.BigoAds)
}

android {
    namespace = "org.bidon.bigoads"
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.BigoAds
    }
}

dependencies {
    compileOnly(project(":bidon"))
    testImplementation(project(":bidon"))

    implementation("com.bigossp:bigo-ads:3.0.3")

    implementation(Dependencies.Google.PlayServicesAds)
}
