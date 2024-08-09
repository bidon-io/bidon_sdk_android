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
    ":adapter:admob",
    ":adapter:amazon",
    ":adapter:applovin",
    ":adapter:bidmachine",
    ":adapter:bigoads",
    ":adapter:dtexchange",
    ":adapter:gam",
    ":adapter:inmobi",
    ":adapter:ironsource",
    ":adapter:meta",
    ":adapter:mintegral",
    ":adapter:mobilefuse",
    ":adapter:unityads",
    ":adapter:vkads",
    ":adapter:vungle",
    ":adapter:yandex",
//    ":adapter:fyber",
//    ":adapter:appsflyer"
)
