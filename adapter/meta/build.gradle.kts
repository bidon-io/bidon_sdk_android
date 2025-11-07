import ext.ADAPTER_VERSION
import ext.Dependencies
import ext.Versions

plugins {
    id("adapter")
}

val adapterSdkVersion = "6.20.0"
val adapterMinor = 0
val adapterSemantic = Versions.semanticVersion

val adapterMainVersion = "$adapterSdkVersion.$adapterMinor$adapterSemantic"

publishAdapter {
    artifactId = "meta-adapter"
    versionName = adapterMainVersion
}

android {
    namespace = "org.bidon.meta"

    defaultConfig {
        ADAPTER_VERSION = adapterMainVersion
    }
}

dependencies {
    implementation("com.facebook.android:audience-network-sdk:$adapterSdkVersion") {
        exclude(group = "com.google.android.gms", module = "play-services-basement")
    }
    implementation(Dependencies.Google.PlayServicesAdsIdentifier)
}
