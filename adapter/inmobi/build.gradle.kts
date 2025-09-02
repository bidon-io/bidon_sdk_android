import ext.ADAPTER_VERSION
import ext.Versions

plugins {
    id("common")
}

publishAdapter {
    artifactId = "inmobi-adapter"
    versionName = Versions.Adapters.Inmobi
}

android {
    namespace = "org.bidon.inmobi"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Inmobi
    }
}

dependencies {
    compileOnly(projects.bidon)
    testImplementation(projects.bidon)

    implementation("com.inmobi.monetization:inmobi-ads-kotlin:10.8.7")
}
