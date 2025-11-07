import ext.ADAPTER_VERSION
import ext.Dependencies
import ext.Versions

plugins {
    id("adapter")
}

val adapterSdkVersion = "8.3.8"
val adapterMinor = 0
val adapterSemantic = Versions.semanticVersion

val adapterMainVersion = "$adapterSdkVersion.$adapterMinor$adapterSemantic"

publishAdapter {
    artifactId = "dtexchange-adapter"
    versionName = adapterMainVersion
}

android {
    namespace = "org.bidon.dtexchange"

    defaultConfig {
        ADAPTER_VERSION = adapterMainVersion
    }
}

dependencies {
    implementation("com.fyber:marketplace-sdk:$adapterSdkVersion")
    implementation(Dependencies.Google.PlayServicesAdsIdentifier)
}
