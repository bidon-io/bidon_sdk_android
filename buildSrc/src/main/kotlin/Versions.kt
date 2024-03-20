object Versions {
    private val major = 0
    private val minor = 4
    private val patch = 29
    private val semantic: String = ""//""-alpha.2"

    val BidonVersionName = mainVersion + semanticVersion

    object Adapters {
        val Admob = "$mainVersion.0" + semanticVersion
        val Applovin = "$mainVersion.0" + semanticVersion
        val BidMachine = "$mainVersion.1" + semanticVersion
        val DTExchange = "$mainVersion.1" + semanticVersion
        val UnityAds = "$mainVersion.1" + semanticVersion
        val BigoAds = "$mainVersion.0" + semanticVersion
        val Mintegral = "$mainVersion.0" + semanticVersion
        val Vungle = "$mainVersion.0" + semanticVersion
        val Meta = "$mainVersion.0" + semanticVersion
        val Inmobi = "$mainVersion.1" + semanticVersion
        val Amazon = "$mainVersion.0" + semanticVersion
        val MobileFuse = "$mainVersion.0" + semanticVersion
        val Gam = "$mainVersion.0" + semanticVersion

        val IronSource = "$mainVersion.0"
        val Appsflyer = "$mainVersion.0"
        val Fyber = "$mainVersion.0"
    }

    private val mainVersion get() = "$major.$minor.$patch"
    private val semanticVersion get() = semantic.takeIf { !it.isNullOrBlank() }.orEmpty()
}
