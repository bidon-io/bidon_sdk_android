import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "amazon-adapter"
    versionName = Versions.Adapters.Amazon
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

    implementation("com.amazon.android:aps-sdk:11.0.1")
    implementation("com.iabtcf:iabtcf-decoder:2.0.10")
}
