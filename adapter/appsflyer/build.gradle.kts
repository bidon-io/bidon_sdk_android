import ext.ADAPTER_SDK_VERSION
import ext.ADAPTER_VERSION
import ext.Versions
import ext.Dependencies

plugins {
    id("common")
    id("publish-adapter")
}

publishAdapter {
    artifactId = "appsflyer-adapter"
    versionName = Versions.Adapters.Appsflyer
}

android {
    namespace = "org.bidon.appsflyer"

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
