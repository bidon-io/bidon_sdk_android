pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://kotlin.bintray.com/kotlinx")
        maven("https://plugins.gradle.org/m2/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // mavenLocal()
        mavenCentral()
        maven("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
        maven("https://artifacts.applovin.com/android")
        maven("https://artifactory.bidmachine.io/bidmachine")
        maven("https://android-sdk.is.com/")

        maven {
            url = uri("https://maven.pkg.github.com/bidon-io/bidon-sdk-android")
            credentials {
                username = System.getenv("GPR_USER")
                password = System.getenv("GPR_TOKEN")
            }
        }
    }
}
rootProject.name = "Bidon SDK"

include(
    ":app",
)
include(
    ":bidon",
    ":adapter:bidmachine",
    ":adapter:admob",
    ":adapter:applovin",
    ":adapter:dtexchange",
    ":adapter:unityads",
    ":adapter:mintegral",
//    ":adapter:fyber",
//    ":adapter:ironsource",
//    ":adapter:appsflyer"
)
