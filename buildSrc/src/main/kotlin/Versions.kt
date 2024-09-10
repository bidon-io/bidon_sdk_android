object Versions {
    private val major = 0
    private val minor = 7
    private val patch = 0
    private val semantic: String = "-next.3"

    val BidonVersionName = mainVersion + semanticVersion

    object Adapters {
        val Admob = "$mainVersion.0" + semanticVersion
        val Amazon = "$mainVersion.0" + semanticVersion
        val Applovin = "$mainVersion.0" + semanticVersion
        val BidMachine = "$mainVersion.0" + semanticVersion
        val BigoAds = "$mainVersion.0" + semanticVersion
        val Chartboost = "$mainVersion.0" + semanticVersion
        val DTExchange = "$mainVersion.0" + semanticVersion
        val Gam = "$mainVersion.0" + semanticVersion
        val Inmobi = "$mainVersion.0" + semanticVersion
        val IronSource = "$mainVersion.0" + semanticVersion
        val Meta = "$mainVersion.0" + semanticVersion
        val Mintegral = "$mainVersion.0" + semanticVersion
        val MobileFuse = "$mainVersion.0" + semanticVersion
        val UnityAds = "$mainVersion.0" + semanticVersion
        val VkAds = "$mainVersion.0" + semanticVersion
        val Vungle = "$mainVersion.0" + semanticVersion
        val Yandex = "$mainVersion.0" + semanticVersion

        val Appsflyer = "$mainVersion.0"
        val Fyber = "$mainVersion.0"
    }

    private val mainVersion get() = "$major.$minor.$patch"
    private val semanticVersion get() = semantic.takeIf { !it.isNullOrBlank() }.orEmpty()
}
