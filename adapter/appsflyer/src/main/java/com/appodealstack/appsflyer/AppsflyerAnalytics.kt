package com.appodealstack.appsflyer

import android.app.Activity
import android.app.Application
import com.appodealstack.bidon.analytics.AdRevenueLogger
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.config.domain.AdapterInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.demands.*
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adrevenue.AppsFlyerAdRevenue
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork
import com.appsflyer.attribution.AppsFlyerRequestListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.coroutines.resume

@Serializable
data class AppsflyerParameters(
    @SerialName("dev_key")
    val devKey: String,
    @SerialName("app_id")
    val appId: String
) : AdapterParameters


private val AppsflyerDemandId = DemandId("appsflyer")

class AppsflyerAnalytics : Adapter, Initializable<AppsflyerParameters>, AdRevenueLogger {
    override val demandId: DemandId = AppsflyerDemandId

    override val adapterInfo = AdapterInfo(
        adapterVersion = "3.2.1",
        bidonSdkVersion = "1.2.3"
    )

    override suspend fun init(activity: Activity, configParams: AppsflyerParameters) {
        val context = activity.applicationContext
        val afRevenueBuilder = AppsFlyerAdRevenue.Builder(context as Application)
        AppsFlyerAdRevenue.initialize(afRevenueBuilder.build())
        suspendCancellableCoroutine { continuation ->
            AppsFlyerLib.getInstance()
                .start(context, configParams.devKey, object : AppsFlyerRequestListener {
                    override fun onSuccess() {
                        continuation.resume(Unit)
                    }

                    override fun onError(p0: Int, p1: String) {
                        logInternal(Tag, "Error while Appsflyer initialization: $p0, $p1.")
                        continuation.resume(Unit)
                    }
                })
        }
    }

    override fun logAdRevenue(mediationNetwork: BNMediationNetwork, ad: Ad) {
        logInternal(Tag, "AdRevenue logged: $ad, mediationNetwork: $mediationNetwork")
        val nonMandatory = mutableMapOf<String, String>().apply {
            ad.dsp?.let { this["demand_source_name"] = it }
            this["ad_type"] = ad.demandAd.adType.adTypeName
            this["auction_round"] = ad.auctionRound.roundName
        }
        val monetizationNetwork = ad.monetizationNetwork ?: "unknown"
        val eventRevenue = ad.price
        val eventRevenueCurrency = ad.currency ?: Currency.getInstance(UsdCurrencyCode)

        AppsFlyerAdRevenue.logAdRevenue(
            monetizationNetwork,
            when (mediationNetwork) {
                BNMediationNetwork.IronSource -> MediationNetwork.ironsource
                BNMediationNetwork.ApplovinMax -> MediationNetwork.applovinmax
                BNMediationNetwork.GoogleAdmob -> MediationNetwork.googleadmob
                BNMediationNetwork.Fyber -> MediationNetwork.fyber
                BNMediationNetwork.Appodeal -> MediationNetwork.appodeal
                BNMediationNetwork.Admost -> MediationNetwork.Admost
                BNMediationNetwork.Topon -> MediationNetwork.Topon
                BNMediationNetwork.Tradplus -> MediationNetwork.Tradplus
                BNMediationNetwork.Yandex -> MediationNetwork.Yandex
            },
            eventRevenueCurrency,
            eventRevenue,
            nonMandatory
        )
    }
}

private const val Tag = "AppsflyerAdapter"