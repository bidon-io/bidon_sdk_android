package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.applovin.ext.adapterVersion
import com.appodealstack.applovin.ext.sdkVersion
import com.appodealstack.applovin.impl.MaxBannerImpl
import com.appodealstack.applovin.impl.MaxInterstitialImpl
import com.appodealstack.applovin.impl.MaxRewardedImpl
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.analytics.MediationNetwork
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.core.parse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.resume

val ApplovinMaxDemandId = DemandId("applovin")

@Suppress("unused")
class ApplovinMaxAdapter :
    Adapter,
    Initializable<ApplovinParameters>,
    AdProvider.Banner<ApplovinBannerAuctionParams>,
    AdProvider.Interstitial<ApplovinFullscreenAdAuctionParams>,
    AdProvider.Rewarded<ApplovinFullscreenAdAuctionParams>,
    AdRevenueSource by AdRevenueSourceImpl(),
    ExtrasSource by ExtrasSourceImpl(),
    MediationNetwork {

    private lateinit var context: Context

    override val mediationNetwork = BNMediationNetwork.ApplovinMax
    override val demandId: DemandId = ApplovinMaxDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: ApplovinParameters): Unit =
        suspendCancellableCoroutine { continuation ->
            val context = activity.applicationContext.also {
                context = it
            }
//            Does not work properly with [configParams.key] research?
//            val instance = AppLovinSdk.getInstance(configParams.key, AppLovinSdkSettings(context), context)
            val instance = AppLovinSdk.getInstance(context)
            instance.settings.setVerboseLogging(true)
            if (!instance.isInitialized) {
                instance.mediationProvider = "max"
                instance.initializeSdk {
                    continuation.resume(Unit)
                }
            } else {
                continuation.resume(Unit)
            }
        }

    override fun parseConfigParam(json: JsonObject): ApplovinParameters = json.parse(ApplovinParameters.serializer())

    override fun interstitial(demandAd: DemandAd, roundId: String): AdSource.Interstitial<ApplovinFullscreenAdAuctionParams> {
        return MaxInterstitialImpl(demandId, demandAd, roundId)
    }

    override fun rewarded(demandAd: DemandAd, roundId: String): AdSource.Rewarded<ApplovinFullscreenAdAuctionParams> {
        return MaxRewardedImpl(demandId, demandAd, roundId)
    }

    override fun banner(demandAd: DemandAd, roundId: String): AdSource.Banner<ApplovinBannerAuctionParams> {
        return MaxBannerImpl(demandId, demandAd, roundId)
    }
}