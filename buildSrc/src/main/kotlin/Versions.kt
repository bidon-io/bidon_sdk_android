object Versions {
    private val major = 0
    private val minor = 2
    private val patch = 1
    private val semantic: String? = "-beta.5"

    val BidonVersionName = mainVersion + semanticVersion

    object Adapters {
        val Admob = "$mainVersion.3"
        val Applovin = "$mainVersion.3"
        val BidMachine = "$mainVersion.3"
        val DTExchange = "$mainVersion.3"
        val UnityAds = "$mainVersion.3"

        val IronSource = "$mainVersion.0"
        val Appsflyer = "$mainVersion.0"
        val Fyber = "$mainVersion.0"
    }

    private val mainVersion get() = "$major.$minor.$patch"
    private val semanticVersion get() = semantic.takeIf { !it.isNullOrBlank() }.orEmpty()
}

