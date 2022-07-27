plugins {
    id("maven-publish")
}

afterEvaluate {
    configure<PublishingExtension> {
        val githubProperties = java.util.Properties()
        githubProperties.load(java.io.FileInputStream(rootProject.file("github.properties")))

        val getArtifactId = project.getArtifactId()
        val getVersionName = project.getVersionName()

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/appodeal/Mads-SDK-Android")
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