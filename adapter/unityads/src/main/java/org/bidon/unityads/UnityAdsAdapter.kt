package org.bidon.unityads

import android.app.Activity
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.sdk.adapter.*
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.unityads.ext.adapterVersion
import org.bidon.unityads.ext.asBidonError
import org.bidon.unityads.ext.sdkVersion
import org.bidon.unityads.impl.UnityAdsAuctionParams
import org.bidon.unityads.impl.UnityAdsInterstitial
import org.bidon.unityads.impl.UnityAdsRewarded
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by Aleksei Cherniaev on 01/03/2023.
 */

internal val UnityAdsDemandId = DemandId("unityads")

class UnityAdsAdapter :
    Adapter,
    Initializable<UnityAdsParameters>,
    AdProvider.Interstitial<UnityAdsAuctionParams>,
    AdProvider.Rewarded<UnityAdsAuctionParams> {

    override val demandId: DemandId = UnityAdsDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: UnityAdsParameters) =
        suspendCancellableCoroutine { continuation ->
            val isTestMode = BuildConfig.DEBUG
            UnityAds.initialize(
                activity.applicationContext,
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
    ): AdSource.Interstitial<UnityAdsAuctionParams> {
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
    ): AdSource.Rewarded<UnityAdsAuctionParams> {
        return UnityAdsRewarded(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }
}

private const val Tag = "UnityAdsAdapter"