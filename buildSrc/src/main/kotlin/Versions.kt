object Versions {
    private val major = 0
    private val minor = 4
    private val patch = 29
    private val semantic: String = ""//""-alpha.2"

    val BidonVersionName = mainVersion + semanticVersion

    object Adapters {
        val Admob = "$mainVersion.2" + semanticVersion
        val Applovin = "$mainVersion.2" + semanticVersion
        val BidMachine = "$mainVersion.2" + semanticVersion
        val DTExchange = "$mainVersion.2" + semanticVersion
        val UnityAds = "$mainVersion.2" + semanticVersion
        val BigoAds = "$mainVersion.1" + semanticVersion
        val Mintegral = "$mainVersion.1" + semanticVersion
        val Vungle = "$mainVersion.1" + semanticVersion
        val Meta = "$mainVersion.1" + semanticVersion
        val Inmobi = "$mainVersion.1" + semanticVersion
        val Amazon = "$mainVersion.1" + semanticVersion
        val MobileFuse = "$mainVersion.0" + semanticVersion
        val Gam = "$mainVersion.1" + semanticVersion

        val IronSource = "$mainVersion.0"
        val Appsflyer = "$mainVersion.0"
        val Fyber = "$mainVersion.0"
    }

    private val mainVersion get() = "$major.$minor.$patch"
    private val semanticVersion get() = semantic.takeIf { !it.isNullOrBlank() }.orEmpty()
}
