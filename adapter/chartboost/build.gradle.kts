import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "chartboost-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Chartboost)
}

android {
    namespace = "org.bidon.chartboost"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Chartboost
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.chartboost:chartboost-sdk:9.8.1")
}
