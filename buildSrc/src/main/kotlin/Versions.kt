object Versions {
    private val major = 0
    private val minor = 2
    private val patch = 1
    private val semantic: String? = "-beta.3"

    val BidonVersionName = mainVersion + semanticVersion

    object Adapters {
        val Admob = "$mainVersion.1"
        val Applovin = "$mainVersion.1"
        val BidMachine = "$mainVersion.1"
        val DTExchange = "$mainVersion.1"
        val UnityAds = "$mainVersion.1"

        val Appsflyer = "$mainVersion.0"
        val IronSource = "$mainVersion.0"
        val Fyber = "$mainVersion.0"
    }

    private val mainVersion get() = "$major.$minor.$patch"
    private val semanticVersion get() = semantic.takeIf { !it.isNullOrBlank() }.orEmpty()
}

