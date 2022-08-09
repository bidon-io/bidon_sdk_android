pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    // repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
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
    }
}
rootProject.name = "BidOn Decorators"

include(
    ":appBidON",
    ":appApplovin",
    ":appIronSource",
    ":appFyber",
)
include(
    ":bidon",
    ":adapter:applovin",
    ":adapter:bidmachine",
    ":adapter:admob",
    ":adapter:fyber",
    ":adapter:ironsource",
    ":adapter:appsflyer"
)
