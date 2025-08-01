import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "amazon-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Amazon)
}

android {
    namespace = "org.bidon.amazon"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Amazon
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.amazon.android:aps-sdk:11.0.0")
    implementation("com.iabtcf:iabtcf-decoder:2.0.10")
}
