import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "mintegral-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Mintegral)
}

android {
    namespace = "org.bidon.mintegral"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Mintegral
    }
}

dependencies {
    compileOnly(project(":bidon"))
    testImplementation(project(":bidon"))

    val version = "16.5.91"
    implementation("com.mbridge.msdk.oversea:reward:$version")
    // If you need to use auction ads, please add this dependency statement.(mbbid)
    implementation("com.mbridge.msdk.oversea:mbbid:$version")
    implementation("com.mbridge.msdk.oversea:mbbanner:$version")
    implementation("com.mbridge.msdk.oversea:newinterstitial:$version")
    implementation("com.mbridge.msdk.oversea:mbsplash:$version")
    implementation("com.mbridge.msdk.oversea:mbnativeadvanced:$version")
    implementation("com.mbridge.msdk.oversea:mbnative:$version")
}
