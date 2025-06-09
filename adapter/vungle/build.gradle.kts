import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "vungle-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Vungle)
}

android {
    namespace = "org.bidon.vungle"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Vungle
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.vungle:vungle-ads:7.5.0")
}
