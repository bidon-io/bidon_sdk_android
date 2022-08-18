package com.appodealstack.appsflyer

import android.app.Activity
import android.app.Application
import com.appodealstack.appsflyer.ext.adapterVersion
import com.appodealstack.appsflyer.ext.sdkVersion
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.analytics.AdRevenueLogger
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.core.parse
import com.appsflyer.AFLogger
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adrevenue.AppsFlyerAdRevenue
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.*

@Serializable
data class AppsflyerParameters(
    @SerialName("dev_key")
    val devKey: String,
    @SerialName("app_id")
    val appId: String
) : AdapterParameters


private val AppsflyerDemandId = DemandId("appsflyer")

@Suppress("unused")
class AppsflyerAnalytics : Adapter, Initializable<AppsflyerParameters>, AdRevenueLogger {
    override val demandId: DemandId = AppsflyerDemandId

    override val adapterInfo by lazy {
        AdapterInfo(
            adapterVersion = adapterVersion,
            sdkVersion = sdkVersion
        )
    }

    override suspend fun init(activity: Activity, configParams: AppsflyerParameters) {
        val context = activity.applicationContext

        /**
         * Main Lib initializing
         */
        val appsFlyer = AppsFlyerLib.getInstance()
        appsFlyer.setAppId(configParams.appId)
        appsFlyer.setLogLevel(AFLogger.LogLevel.VERBOSE)
        appsFlyer.init(configParams.devKey, null, context)
        appsFlyer.logEvent(context, null, null)
        appsFlyer.start(context, configParams.devKey)

        /**
         * AdRevenue Lib initializing
         */
        (context as? Application)?.let {
            val builder = AppsFlyerAdRevenue.Builder(it)
            AppsFlyerAdRevenue.initialize(builder.build())
        }
    }

    override fun logAdRevenue(ad: Ad) {
        logInfo(Tag, "AdRevenue logged: $ad")
        val nonMandatory = mutableMapOf<String, String>().apply {
            ad.dsp?.let { this["demand_source_name"] = it }
            this["ad_type"] = ad.demandAd.adType.adTypeName
            this["auction_round"] = ad.roundId
        }
        val monetizationNetwork = ad.monetizationNetwork ?: "unknown"
        val eventRevenue = ad.price
        val eventRevenueCurrency = ad.currency ?: Currency.getInstance(UsdCurrencyCode)

        AppsFlyerAdRevenue.logAdRevenue(
            monetizationNetwork,
            MediationNetwork.appodeal, // @see https://appodeal.slack.com/archives/C02PE4GAFU0/p1660830733326759
            eventRevenueCurrency,
            eventRevenue,
            nonMandatory
        )
    }

    override fun parseConfigParam(json: JsonObject): AppsflyerParameters = json.parse(AppsflyerParameters.serializer())

}

private const val Tag = "AppsflyerAdapter"