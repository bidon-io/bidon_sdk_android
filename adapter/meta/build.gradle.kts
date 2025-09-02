import ext.ADAPTER_VERSION
import ext.Dependencies
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "meta-adapter"
    versionName = Versions.Adapters.Meta
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

    implementation("com.facebook.android:audience-network-sdk:6.20.0") {
        exclude(group = "com.google.android.gms", module = "play-services-basement")
    }
    implementation(Dependencies.Google.PlayServicesAdsIdentifier)
}
