package org.bidon.unityads

import android.content.Context
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.unityads.ext.adapterVersion
import org.bidon.unityads.ext.asBidonError
import org.bidon.unityads.ext.sdkVersion
import org.bidon.unityads.impl.UnityAdsBanner
import org.bidon.unityads.impl.UnityAdsBannerAuctionParams
import org.bidon.unityads.impl.UnityAdsFullscreenAuctionParams
import org.bidon.unityads.impl.UnityAdsInterstitial
import org.bidon.unityads.impl.UnityAdsRewarded
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by Bidon Team on 01/03/2023.
 */

internal val UnityAdsDemandId = DemandId("unityads")

/**
 * [Documentation](https://docs.unity.com/ads/en/manual/InitializingTheAndroidSDK)
 */
class UnityAdsAdapter :
    Adapter,
    Initializable<UnityAdsParameters>,
    AdProvider.Banner<UnityAdsBannerAuctionParams>,
    AdProvider.Interstitial<UnityAdsFullscreenAuctionParams>,
    AdProvider.Rewarded<UnityAdsFullscreenAuctionParams> {

    override val demandId: DemandId = UnityAdsDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: UnityAdsParameters) =
        suspendCancellableCoroutine { continuation ->
            val isTestMode = BuildConfig.DEBUG
            UnityAds.initialize(
                context,
                configParams.unityGameId,
                isTestMode,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        continuation.resume(Unit)
                    }

                    override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                        logError(Tag, "Error while initialization: $message, $error", error.asBidonError())
                        continuation.resumeWithException(error.asBidonError())
                    }
                }
            )
        }

    override fun parseConfigParam(json: String): UnityAdsParameters {
        return UnityAdsParameters(
            unityGameId = JSONObject(json).optString("game_id")
        )
    }

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<UnityAdsFullscreenAuctionParams> {
        return UnityAdsInterstitial(
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
    ): AdSource.Rewarded<UnityAdsFullscreenAuctionParams> {
        return UnityAdsRewarded(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }

    override fun banner(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Banner<UnityAdsBannerAuctionParams> {
        return UnityAdsBanner(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }
}

private const val Tag = "UnityAdsAdapter"