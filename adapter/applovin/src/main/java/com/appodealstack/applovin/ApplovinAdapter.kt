package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.applovin.ext.adapterVersion
import com.appodealstack.applovin.ext.sdkVersion
import com.appodealstack.applovin.impl.ApplovinBannerImpl
import com.appodealstack.applovin.impl.ApplovinInterstitialImpl
import com.appodealstack.applovin.impl.ApplovinRewardedImpl
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.analytics.MediationNetwork
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.core.parse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.resume

val ApplovinDemandId = DemandId("applovin")

@Suppress("unused")
class ApplovinAdapter :
    Adapter,
    Initializable<ApplovinParameters>,
    AdProvider.Banner<ApplovinBannerAuctionParams>,
    AdProvider.Interstitial<ApplovinFullscreenAdAuctionParams>,
    AdProvider.Rewarded<ApplovinFullscreenAdAuctionParams>,
    AdRevenueSource by AdRevenueSourceImpl(),
    ExtrasSource by ExtrasSourceImpl(),
    MediationNetwork {

    private lateinit var context: Context
    private var appLovinSdk: AppLovinSdk? = null

    override val mediationNetwork = BNMediationNetwork.Applovin
    override val demandId: DemandId = ApplovinDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: ApplovinParameters): Unit =
        suspendCancellableCoroutine { continuation ->
            val context = activity.applicationContext.also {
                context = it
            }
            val instance = AppLovinSdk.getInstance(context).also {
                appLovinSdk = it
            }
            instance.settings.setVerboseLogging(true)
            if (!instance.isInitialized) {
                instance.initializeSdk {
                    continuation.resume(Unit)
                }
            } else {
                continuation.resume(Unit)
            }
        }

    override fun parseConfigParam(json: JsonObject): ApplovinParameters = json.parse(ApplovinParameters.serializer())

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<ApplovinFullscreenAdAuctionParams> {
        return ApplovinInterstitialImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            appLovinSdk = requireNotNull(appLovinSdk),
            auctionId = auctionId
        )
    }

    override fun rewarded(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Rewarded<ApplovinFullscreenAdAuctionParams> {
        return ApplovinRewardedImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            appLovinSdk = requireNotNull(appLovinSdk),
            auctionId = auctionId
        )
    }

    override fun banner(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Banner<ApplovinBannerAuctionParams> {
        return ApplovinBannerImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            appLovinSdk = requireNotNull(appLovinSdk),
            auctionId = auctionId
        )
    }
}