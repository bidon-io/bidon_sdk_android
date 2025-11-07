import ext.ADAPTER_VERSION
import ext.Dependencies
import ext.Versions

plugins {
    id("adapter")
}

val adapterSdkVersion = "11.0.1"
val adapterMinor = 0
val adapterSemantic = Versions.semanticVersion

val adapterMainVersion = "$adapterSdkVersion.$adapterMinor$adapterSemantic"

publishAdapter {
    artifactId = "amazon-adapter"
    versionName = adapterMainVersion
}

android {
    namespace = "org.bidon.amazon"

    defaultConfig {
        ADAPTER_VERSION = adapterMainVersion
    }
}

dependencies {
    implementation("com.amazon.android:aps-sdk:$adapterSdkVersion")
    implementation(Dependencies.Others.IabTcfDecoder)
}
