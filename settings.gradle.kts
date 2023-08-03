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
        maven {
            url = uri("https://artifacts.applovin.com/android")
        }
        maven {
            name = "BidMachine Ads maven repository"
            url = uri("https://artifactory.bidmachine.io/bidmachine")
        }
        maven {
            url = uri("https://android-sdk.is.com/")
        }
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
    ":adapter:bigoads",
    ":adapter:mintegral",
//    ":adapter:fyber",
//    ":adapter:ironsource",
//    ":adapter:appsflyer"
)
