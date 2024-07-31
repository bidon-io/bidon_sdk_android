import ext.ADAPTER_SDK_VERSION
import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "appsflyer-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Appsflyer)
}

android {
    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Appsflyer
        ADAPTER_SDK_VERSION = Dependencies.SdkAdapter.AppsflyerVersion
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation(Dependencies.SdkAdapter.Appsflyer)
    implementation(Dependencies.SdkAdapter.AppsflyerAdRevenue)
}
