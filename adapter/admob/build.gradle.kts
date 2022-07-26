plugins {
    id("common")
}

dependencies {
    compileOnly(project(":bidon"))
    implementation("com.google.android.gms:play-services-ads:21.1.0")
}