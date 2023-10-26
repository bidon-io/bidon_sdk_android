package org.bidon.unityads

import android.content.Context
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.metadata.MetaData
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.adapter.SupportsTestMode
import org.bidon.sdk.adapter.impl.SupportsTestModeImpl
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.regulation.Regulation
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
 * Created by Aleksei Cherniaev on 01/03/2023.
 */

internal val UnityAdsDemandId = DemandId("unityads")

/**
 * [Documentation](https://docs.unity.com/ads/en/manual/InitializingTheAndroidSDK)
 */
class UnityAdsAdapter :
    Adapter,
    SupportsRegulation,
    SupportsTestMode by SupportsTestModeImpl(),
    Initializable<UnityAdsParameters>,
    AdProvider.Banner<UnityAdsBannerAuctionParams>,
    AdProvider.Interstitial<UnityAdsFullscreenAuctionParams>,
    AdProvider.Rewarded<UnityAdsFullscreenAuctionParams> {

    private var context: Context? = null
    override val demandId: DemandId = UnityAdsDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: UnityAdsParameters) =
        suspendCancellableCoroutine { continuation ->
            this.context = context
            UnityAds.initialize(
                context,
                configParams.unityGameId,
                isTestMode,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        continuation.resume(Unit)
                    }

                    override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                        logError(TAG, "Error while initialization: $message, $error", error.asBidonError())
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

    override fun updateRegulation(regulation: Regulation) {
        MetaData(context).also { data ->
            if (regulation.gdprApplies) {
                data.set("gdpr.consent", regulation.hasGdprConsent)
            }
            if (!regulation.ccpaApplies) {
                data.set("privacy.consent", regulation.hasCcpaConsent)
            }
            if (regulation.coppaApplies) {
                data.set("privacy.useroveragelimit", true)
            }
        }.commit()
    }

    override fun interstitial(): AdSource.Interstitial<UnityAdsFullscreenAuctionParams> {
        return UnityAdsInterstitial()
    }

    override fun rewarded(): AdSource.Rewarded<UnityAdsFullscreenAuctionParams> {
        return UnityAdsRewarded()
    }

    override fun banner(): AdSource.Banner<UnityAdsBannerAuctionParams> {
        return UnityAdsBanner()
    }
}

private const val TAG = "UnityAdsAdapter"