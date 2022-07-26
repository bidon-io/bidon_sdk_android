plugins {
    id("common")
}

dependencies {
    compileOnly(project(":bidon"))
    implementation("com.appsflyer:af-android-sdk:6.7.0")
    implementation("com.appsflyer:adrevenue:6.5.4")
}