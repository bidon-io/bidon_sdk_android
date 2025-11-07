package ext

object Versions {
    private const val major = 0
    private const val minor = 12
    private const val patch = 0
    private const val semantic: String = ""

    val BidonVersionName = mainVersion + semanticVersion

    object AdapterSupportedCoreRange {
        const val Min = "0.10.0"
        const val Max = "1.0.0"
    }

    object ThirdPartyMediationAdapters {
        val ApplovinMax = "$mainVersion.0$semanticVersion"
        val LevelPlay = "$mainVersion.0$semanticVersion"
    }

    private val mainVersion get() = "$major.$minor.$patch"
    val semanticVersion get() = semantic.takeIf { it.isNotBlank() }.orEmpty()
}
