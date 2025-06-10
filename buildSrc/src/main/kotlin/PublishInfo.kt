import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

class PublishInfo {
    var groupId: String = error("GroupId is not unspecified")
    var artifactId: String = error("ArtifactId is not unspecified")
    var versionName: String = error("VersionName is not unspecified")

    override fun toString(): String {
        return "$artifactId:$versionName"
    }
}

fun Project.publishInfo(info: PublishInfo.() -> Unit) {
    val publishInfo = PublishInfo().apply(info)
    this.extra.set(AdapterGroupId, publishInfo.groupId)
    this.extra.set(AdapterArtifactId, publishInfo.artifactId)
    this.extra.set(AdapterVersionName, publishInfo.versionName)
}

fun Project.getGroupId(default: String) = if (this.extra.has(AdapterGroupId)) this.extra[AdapterGroupId] as? String else default
fun Project.getArtifactId() = this.extra.get(AdapterArtifactId) as String
fun Project.getVersionName() = this.extra.get(AdapterVersionName) as String

private const val AdapterGroupId = "AdapterGroupId"
private const val AdapterArtifactId = "AdapterArtifactId"
private const val AdapterVersionName = "AdapterVersionName"
