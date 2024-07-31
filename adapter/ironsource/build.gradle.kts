import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "ironsource-decorator")
    this.set("AdapterVersionName", Versions.Adapters.IronSource)
}

android {
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.IronSource
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.ironsource.sdk:mediationsdk:7.2.7")
}
