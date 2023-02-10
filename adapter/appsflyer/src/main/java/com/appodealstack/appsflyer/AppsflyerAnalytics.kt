package com.appodealstack.appsflyer

import android.app.Activity
import android.app.Application
import com.appodealstack.appsflyer.ext.adapterVersion
import com.appodealstack.appsflyer.ext.sdkVersion
import com.appodealstack.bidon.data.models.config.AdapterInfo
import com.appodealstack.bidon.domain.adapter.Adapter
import com.appodealstack.bidon.domain.adapter.AdapterParameters
import com.appodealstack.bidon.domain.adapter.Initializable
import com.appodealstack.bidon.domain.analytic.AdRevenueLogger
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.common.DemandId
import com.appodealstack.bidon.domain.common.UsdCurrencyCode
import com.appodealstack.bidon.domain.logging.impl.logInfo
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
        val monetizationNetwork = ad.monetizationNetwork ?: "unknown"
        val eventRevenue = ad.price
        val eventRevenueCurrency = ad.currency ?: Currency.getInstance(UsdCurrencyCode)

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
