object Versions {
    private val major = 0
    private val minor = 2
    private val patch = 2
    private val semantic: String? = null//"-beta.20"

    val BidonVersionName = mainVersion + semanticVersion

    object Adapters {
        val Admob = "$mainVersion.0"
        val Applovin = "$mainVersion.0"
        val BidMachine = "$mainVersion.0"
        val DTExchange = "$mainVersion.0"
        val UnityAds = "$mainVersion.0"

        val IronSource = "$mainVersion.0"
        val Appsflyer = "$mainVersion.0"
        val Fyber = "$mainVersion.0"
    }

    private val mainVersion get() = "$major.$minor.$patch"
    private val semanticVersion get() = semantic.takeIf { !it.isNullOrBlank() }.orEmpty()
}
