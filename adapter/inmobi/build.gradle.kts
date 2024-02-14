import ext.ADAPTER_VERSION

plugins {
    id("common")
    id("publish-adapter")
}

project.extra.apply {
    this.set("AdapterArtifactId", "inmobi-adapter")
    this.set("AdapterVersionName", Versions.Adapters.Inmobi)
}

android {
    namespace = "org.bidon.inmobi"

    defaultConfig {
        ADAPTER_VERSION = Versions.Adapters.Inmobi
    }
}

dependencies {
    compileOnly(project(":bidon"))
    testImplementation(project(":bidon"))

    implementation("com.inmobi.monetization:inmobi-ads-kotlin:10.6.2")
}
