enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // mavenLocal()
        mavenCentral()
        maven(url = "https://artifactory.bidon.org/bidon")
        maven(url = "https://artifactory.bidon.org/bidon-private") {
            credentials {
                username = System.getenv("BDN_USER")
                password = System.getenv("BDN_PASSWORD")
            }
        }
        maven(url = "https://maven.pkg.github.com/bidon-io/bidon-sdk-android") {
            credentials {
                username = System.getenv("GPR_USER")
                password = System.getenv("GPR_TOKEN")
            }
        }
    }
}
rootProject.name = "BidonSDK"

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
    ":adapter:vungle",
    ":adapter:bigoads",
    ":adapter:mintegral",
    ":adapter:meta",
    ":adapter:inmobi",
    ":adapter:amazon",
    ":adapter:mobilefuse",
    ":adapter:gam",
//    ":adapter:fyber",
//    ":adapter:ironsource",
//    ":adapter:appsflyer"
)
