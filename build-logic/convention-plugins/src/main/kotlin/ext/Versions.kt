package ext

object Versions {
    private val major = 0
    private val minor = 10
    private val patch = 0
    private val semantic: String = ""

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

    object ThirdPartyMediationAdapters {
        val ApplovinMax = "$mainVersion.0" + semanticVersion
        val LevelPlay = "$mainVersion.0" + semanticVersion
    }

    private val mainVersion get() = "$major.$minor.$patch"
    private val semanticVersion get() = semantic.takeIf { !it.isNullOrBlank() }.orEmpty()
}
