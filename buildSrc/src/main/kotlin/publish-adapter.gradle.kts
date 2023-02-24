plugins {
    id("maven-publish")
}
val githubProperties = java.util.Properties()
val githubCredentialFile: File = rootProject.file("github.properties")
if (githubCredentialFile.exists()) {
    githubProperties.load(java.io.FileInputStream(githubCredentialFile))
}

afterEvaluate {
    configure<PublishingExtension> {
        val getArtifactId = project.getArtifactId()
        val getVersionName = project.getVersionName()

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/bidon-io/bidon-sdk-android")
                credentials {
                    username = githubProperties["gpr.usr"] as? String ?: System.getenv("GPR_USER")
                    password = githubProperties["gpr.key"] as? String ?: System.getenv("GPR_TOKEN")
                }
            }
        }

        publications {
            register<MavenPublication>("gpr") {
                afterEvaluate {
                    from(components["release"])
                }
                groupId = "org.bidon" // Replace with group ID
                artifactId = getArtifactId
                version = getVersionName
            }
        }
    }
}