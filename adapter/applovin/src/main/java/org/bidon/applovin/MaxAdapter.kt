package org.bidon.applovin

import android.app.Activity
import android.content.Context
import com.applovin.sdk.AppLovinSdk
import org.bidon.applovin.ext.adapterVersion
import org.bidon.applovin.ext.sdkVersion
import org.bidon.applovin.impl.MaxBannerImpl
import org.bidon.applovin.impl.MaxInterstitialImpl
import org.bidon.applovin.impl.MaxRewardedImpl
import org.bidon.sdk.adapter.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

val MaxDemandId = DemandId("max")

@Suppress("unused")
class MaxAdapter :
    Adapter,
    Initializable<MaxParameters>,
    AdProvider.Banner<MaxBannerAuctionParams>,
    AdProvider.Interstitial<MaxFullscreenAdAuctionParams>,
    AdProvider.Rewarded<MaxFullscreenAdAuctionParams> {

    private lateinit var context: Context

    override val demandId: DemandId = MaxDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: MaxParameters): Unit =
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

    override fun parseConfigParam(json: String): MaxParameters {
        val jsonObject = JSONObject(json)
        return MaxParameters(
            key = jsonObject.getString("app_key"),
        )
    }

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<MaxFullscreenAdAuctionParams> {
        return MaxInterstitialImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }

    override fun rewarded(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Rewarded<MaxFullscreenAdAuctionParams> {
        return MaxRewardedImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }

    override fun banner(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Banner<MaxBannerAuctionParams> {
        return MaxBannerImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }
}