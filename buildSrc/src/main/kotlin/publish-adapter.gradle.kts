plugins {
    id("maven-publish")
}
val githubProperties = java.util.Properties()
githubProperties.load(java.io.FileInputStream(rootProject.file("github.properties")))

afterEvaluate {
    configure<PublishingExtension> {

        val getArtifactId = project.getArtifactId()
        val getVersionName = project.getVersionName()

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/appodeal/BidOn-SDK-Android")
                credentials {
                    username = githubProperties["gpr.usr"] as? String ?: System.getenv("GPR_USER")
                    password = githubProperties["gpr.key"] as? String ?: System.getenv("GPR_API_KEY")
                }
            }
        }

        publications {
            register<MavenPublication>("gpr") {
                groupId = "com.appodealstack.bidon" // Replace with group ID
                artifactId = getArtifactId
                version = getVersionName
                artifact("$buildDir/outputs/aar/${project.name}-release.aar")
            }
        }
    }
}