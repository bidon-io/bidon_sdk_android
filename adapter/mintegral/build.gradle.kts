import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("adapter")
}

val adapterSdkVersion = "16.9.91"
val adapterMinor = 0
val adapterSemantic = Versions.semanticVersion

val adapterMainVersion = "$adapterSdkVersion.$adapterMinor$adapterSemantic"

publishAdapter {
    artifactId = "mintegral-adapter"
    versionName = adapterMainVersion
}

android {
    namespace = "org.bidon.mintegral"

    defaultConfig {
        ADAPTER_VERSION = adapterMainVersion
    }
}

dependencies {
    implementation("com.mbridge.msdk.oversea:mbridge_android_sdk:$adapterSdkVersion")
}
