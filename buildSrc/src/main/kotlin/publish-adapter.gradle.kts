import java.io.FileInputStream
import java.util.Properties

plugins {
    id("maven-publish")
    id("common") apply false
}

val githubProperties = Properties()
val githubCredentialFile: File = rootProject.file("github.properties")
if (githubCredentialFile.exists()) {
    githubProperties.load(FileInputStream(githubCredentialFile))
}

afterEvaluate {
    val dokkaJar by tasks.registering(Jar::class) {
        group = "documentation"
        dependsOn(tasks.getByName("dokkaJavadoc"))
        include(javadoc.ClassesList.javaDocsAllowList)
        archiveClassifier.set("javadoc")
        from("$buildDir/dokka/javadoc")
    }

    val sourcesJar by tasks.registering(Jar::class) {
        group = "documentation"
        archiveClassifier.set("sources")
        include(javadoc.ClassesList.javaDocsAllowList)
        from(android.sourceSets.getByName("main").java.srcDirs)
    }
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
                    from(components["productionRelease"])
                }
                artifact(dokkaJar)
                artifact(sourcesJar)
                groupId = "org.bidon" // Replace with group ID
                artifactId = getArtifactId
                version = getVersionName
            }
        }
    }
}