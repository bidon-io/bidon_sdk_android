package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import com.appodealstack.applovin.ext.adapterVersion
import com.appodealstack.applovin.ext.sdkVersion
import com.appodealstack.applovin.impl.ApplovinBannerImpl
import com.appodealstack.applovin.impl.ApplovinInterstitialImpl
import com.appodealstack.applovin.impl.ApplovinRewardedImpl
import com.appodealstack.bidon.BidOn
import com.appodealstack.bidon.adapter.AdProvider
import com.appodealstack.bidon.adapter.AdSource
import com.appodealstack.bidon.adapter.Adapter
import com.appodealstack.bidon.adapter.Initializable
import com.appodealstack.bidon.ads.DemandAd
import com.appodealstack.bidon.ads.DemandId
import com.appodealstack.bidon.adapter.AdapterInfo
import com.appodealstack.bidon.logs.logging.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

val ApplovinDemandId = DemandId("applovin")

@Suppress("unused")
class ApplovinAdapter :
    Adapter,
    Initializable<ApplovinParameters>,
    AdProvider.Banner<ApplovinBannerAuctionParams>,
    AdProvider.Interstitial<ApplovinFullscreenAdAuctionParams>,
    AdProvider.Rewarded<ApplovinFullscreenAdAuctionParams> {

    private lateinit var context: Context
    private var applovinSdk: AppLovinSdk? = null

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
            val instance = AppLovinSdk.getInstance(configParams.key, AppLovinSdkSettings(context), context).also {
                applovinSdk = it
            }
            instance.settings.setVerboseLogging(BidOn.loggerLevel != Logger.Level.Off)
            if (!instance.isInitialized) {
                instance.initializeSdk {
                    continuation.resume(Unit)
                }
            } else {
                continuation.resume(Unit)
            }
        }

    override fun parseConfigParam(json: String): ApplovinParameters {
        val jsonObject = JSONObject(json)
        return ApplovinParameters(
            key = jsonObject.getString("app_key"),
        )
    }

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<ApplovinFullscreenAdAuctionParams> {
        return ApplovinInterstitialImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            applovinSdk = requireNotNull(applovinSdk),
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
            applovinSdk = requireNotNull(applovinSdk),
            auctionId = auctionId
        )
    }

    override fun banner(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Banner<ApplovinBannerAuctionParams> {
        return ApplovinBannerImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            applovinSdk = requireNotNull(applovinSdk),
            auctionId = auctionId
        )
    }
}