package org.bidon.applovin

import android.content.Context
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.applovin.ext.adapterVersion
import org.bidon.applovin.ext.sdkVersion
import org.bidon.applovin.impl.ApplovinBannerImpl
import org.bidon.applovin.impl.ApplovinInterstitialImpl
import org.bidon.applovin.impl.ApplovinRewardedImpl
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.*
import org.bidon.sdk.logs.logging.Logger
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

    private var applovinSdk: AppLovinSdk? = null

    override val demandId: DemandId = ApplovinDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: ApplovinParameters): Unit =
        suspendCancellableCoroutine { continuation ->
            val instance =
                AppLovinSdk.getInstance(configParams.key, AppLovinSdkSettings(context), context)
                    .also {
                        applovinSdk = it
                    }
            instance.settings.setVerboseLogging(BidonSdk.loggerLevel != Logger.Level.Off)
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

    override fun banner(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Banner<ApplovinBannerAuctionParams> {
        return ApplovinBannerImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            applovinSdk = requireNotNull(applovinSdk),
            auctionId = auctionId
        )
    }
}