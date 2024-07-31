import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "meta-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Meta)
}

android {
    namespace = "org.bidon.meta"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Meta
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.facebook.android:audience-network-sdk:6.17.0") {
        exclude(group = "com.google.android.gms", module = "play-services-basement")
    }
    implementation(Dependencies.Google.PlayServicesAdsIdentifier)
}
