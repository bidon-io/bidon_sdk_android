plugins {
    id("common")
}

dependencies {
    api(project(":bidon"))
    implementation("com.ironsource.sdk:mediationsdk:7.2.3.1")
}