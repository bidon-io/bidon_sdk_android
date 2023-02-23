package org.bidon.appsflyer

import android.app.Activity
import android.app.Application
import org.bidon.appsflyer.ext.adapterVersion
import org.bidon.appsflyer.ext.sdkVersion
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.logs.analytic.AdRevenueLogger
import org.bidon.sdk.logs.logging.impl.logInfo
import com.appsflyer.AFLogger
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adrevenue.AppsFlyerAdRevenue
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork
import org.json.JSONObject
import java.util.*

data class AppsflyerParameters(
    val devKey: String,
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
            this["ad_type"] = ad.demandAd.adType.code
            this["auction_round"] = ad.roundId
        }
        val monetizationNetwork = ad.networkName ?: "unknown"
        val eventRevenue = ad.eCPM
        val eventRevenueCurrency = ad.currency ?: Currency.getInstance("USD")

        AppsFlyerAdRevenue.logAdRevenue(
            monetizationNetwork, // demandId
            MediationNetwork.appodeal, // @see https://appodeal.slack.com/archives/C02PE4GAFU0/p1660830733326759
            eventRevenueCurrency,
            eventRevenue,
            nonMandatory
        )
    }

    override fun parseConfigParam(json: String): AppsflyerParameters {
        val jsonObject = JSONObject(json)
        return AppsflyerParameters(
            devKey = jsonObject.getString("dev_key"),
            appId = jsonObject.getString("app_id"),
        )
    }
}

private const val Tag = "AppsflyerAdapter"
