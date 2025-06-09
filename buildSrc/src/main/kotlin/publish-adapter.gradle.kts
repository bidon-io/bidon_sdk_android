import java.io.FileInputStream
import java.util.Properties

plugins {
    id("maven-publish")
    id("common") apply false
    signing
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
    publishing {
        val getGroupId = project.getGroupId(default = "org.bidon")
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
                    println("Artifactory repo: $url. $getArtifactId, $getVersionName")
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
            maven {
                name = "MavenCentral"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    val mavenUser: String? by project
                    val mavenPassword: String? by project
                    username = mavenUser
                    password = mavenPassword
                }
            }
        }
        publications {
            register<MavenPublication>("gpr") {
                afterEvaluate {
                    from(components["productionRelease"])
                }
                artifact(dokkaJar)
                pom {
                    groupId = getGroupId
                    artifactId = getArtifactId
                    version = getVersionName
                    name.set(project.name)
                    description.set(project.description)
                    url.set("https://bidon.org/")
                    scm {
                        url.set("https://github.com/bidon-io/bidon_sdk_android.git")
                        connection.set("scm:git:github.com/bidon-io/bidon_sdk_android.git")
                        developerConnection.set("scm:git:ssh://github.com/bidon-io/bidon_sdk_android.git")
                    }
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    organization {
                        name.set("Bidon")
                        url.set("https://bidon.org/")
                    }
                    developers {
                        developer {
                            id.set("bidon")
                            name.set("Bidon Dev Team")
                            email.set("dev@bidon.org")
                            url.set("https://bidon.org/")
                        }
                    }
                }
            }
        }
        signing {
            // TODO Need to finish CI-implementation through [maven.yml]
            isRequired = gradle.taskGraph.hasTask("publishGprPublicationToMavenCentralRepository")
            val keyId: String? by project
            val key: String? by project
            val password: String? by project
            useInMemoryPgpKeys(keyId, key, password)
            sign(publishing.publications)
        }
    }
}