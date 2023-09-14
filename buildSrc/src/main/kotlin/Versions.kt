object Versions {
    private val major = 0
    private val minor = 3
    private val patch = 3
    private val semantic: String? = null//"-alpha.1"

    val BidonVersionName = mainVersion + semanticVersion

    object Adapters {
        val Admob = "$mainVersion.0"
        val Applovin = "$mainVersion.0"
        val BidMachine = "$mainVersion.0"
        val DTExchange = "$mainVersion.0"
        val UnityAds = "$mainVersion.0"
        val BigoAds = "$mainVersion.0"
        val Mintegral = "$mainVersion.0"
        val Vungle = "$mainVersion.0"
        val Meta = "$mainVersion.0"
        val Inmobi = "$mainVersion.0"

        val IronSource = "$mainVersion.0"
        val Appsflyer = "$mainVersion.0"
        val Fyber = "$mainVersion.0"
    }

    private val mainVersion get() = "$major.$minor.$patch"
    private val semanticVersion get() = semantic.takeIf { !it.isNullOrBlank() }.orEmpty()
}
