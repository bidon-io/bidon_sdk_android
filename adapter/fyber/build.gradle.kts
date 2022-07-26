plugins {
    id("common")
}

dependencies {
    api(project(":bidon"))
    implementation("com.fyber.omsdk:om-sdk:1.3.28")
    implementation("com.fyber:fairbid-sdk:3.28.1")
    implementation("com.fyber:marketplace-sdk:8.1.3")
}