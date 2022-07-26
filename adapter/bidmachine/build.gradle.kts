plugins {
    id("common")
}

dependencies {
    compileOnly(project(":bidon"))
    implementation("io.bidmachine:ads:1.9.7")
    implementation("io.bidmachine:ads.adapters.admob:1.9.4.22")
}