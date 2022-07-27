import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

class PublishInfo {
    var artifactId: String = error("ArtifactId is not unspecified")
    var versionName: String = error("VersionName is not unspecified")

    override fun toString(): String {
        return "$artifactId:$versionName"
    }
}

fun Project.publishInfo(info: PublishInfo.() -> Unit) {
    val publishInfo = PublishInfo().apply(info)
    this.extra.set(AdapterArtifactId, publishInfo.artifactId)
    this.extra.set(AdapterVersionName, publishInfo.versionName)
}

fun Project.getArtifactId() = this.extra.get(AdapterArtifactId) as String
fun Project.getVersionName() = this.extra.get(AdapterVersionName) as String

private const val AdapterArtifactId = "AdapterArtifactId"
private const val AdapterVersionName = "AdapterVersionName"
