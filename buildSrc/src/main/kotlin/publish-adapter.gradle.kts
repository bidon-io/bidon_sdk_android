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
    publishing {
        val getArtifactId = project.getArtifactId()
        val getVersionName = project.getVersionName()

        repositories {
            val repo: String? by project
            val uname: String? by project
            val upassword: String? by project

            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/bidon-io/bidon-sdk-android")
                credentials {
                    username = githubProperties["gpr.usr"] as? String ?: System.getenv("GPR_USER")
                    password = githubProperties["gpr.key"] as? String ?: System.getenv("GPR_TOKEN")
                }
            }
            maven {
                name = "Bidon"
                repo?.let {
                    url = uri("https://artifactory.bidon.org/artifactory/$repo")
                    println("Artifactory repo: $url")
                    credentials {
                        uname?.let {
                            username = uname
                            println("Artifactory username: $uname")
                        }
                        upassword?.let {
                            password = upassword
                            println("Artifactory password: $upassword")
                        }
                    }
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
                pom.withXml {
                    asNode().apply {
                        appendNode("name", project.name)
                        appendNode("description", project.description)
                        appendNode("url", "https://bidon.org/")
                        appendNode("licenses")
                            .appendNode("license").apply {
                                appendNode("name", "Bidon SDK License Agreement")
                                appendNode("url", "https://github.com/bidon-io/bidon-sdk-android/blob/main/LICENSE.md")
                            }
                        appendNode("scm").apply {
                            appendNode(
                                "connection",
                                "scm:git:github.com/bidon-io/bidon-sdk-android.git"
                            )
                            appendNode(
                                "developerConnection",
                                "scm:git:ssh://github.com/bidon-io/bidon-sdk-android.git"
                            )
                            appendNode("url", "https://github.com/bidon-io/bidon-sdk-android.git")
                        }
                        appendNode("developers")
                            .appendNode("developer").apply {
                                appendNode("name", "Bidon")
                                appendNode("url", "https://bidon.org/")
                            }
                    }
                }
            }
        }
    }
}