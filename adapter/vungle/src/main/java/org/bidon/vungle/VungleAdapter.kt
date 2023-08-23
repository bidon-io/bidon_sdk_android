package org.bidon.vungle

import android.content.Context
import com.vungle.warren.InitCallback
import com.vungle.warren.Vungle
import com.vungle.warren.error.VungleException
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
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.regulation.Gdpr
import org.bidon.sdk.regulation.Regulation
import org.bidon.vungle.ext.adapterVersion
import org.bidon.vungle.ext.sdkVersion
import org.bidon.vungle.impl.VungleBannerImpl
import org.bidon.vungle.impl.VungleInterstitialImpl
import org.bidon.vungle.impl.VungleRewardedImpl
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by Aleksei Cherniaev on 14/07/2023.
 */
internal val VungleDemandId = DemandId("vungle")

class VungleAdapter :
    Adapter,
    Initializable<VungleParameters>,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<VungleBannerAuctionParams>,
    SupportsRegulation,
    AdProvider.Interstitial<VungleFullscreenAuctionParams>,
    AdProvider.Rewarded<VungleFullscreenAuctionParams> {
    override val demandId: DemandId = VungleDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: VungleParameters) = suspendCancellableCoroutine { continuation ->
        Vungle.init(
            configParams.appId, context,
            object : InitCallback {
                override fun onSuccess() {
                    continuation.resume(Unit)
                }

                override fun onError(exception: VungleException?) {
                    logError(TAG, "Error while initialization", exception)
                    continuation.resumeWithException(exception ?: BidonError.SdkNotInitialized)
                }

                override fun onAutoCacheAdAvailable(placementId: String?) {
                }
            }
        )
    }

    override fun parseConfigParam(json: String): VungleParameters {
        return VungleParameters(
            appId = JSONObject(json).getString("app_id")
        )
    }

    override fun updateRegulation(regulation: Regulation) {
        Vungle.updateConsentStatus(
            when (regulation.gdpr) {
                Gdpr.Unknown -> Vungle.Consent.OPTED_OUT
                Gdpr.Denied -> Vungle.Consent.OPTED_OUT
                Gdpr.Given -> Vungle.Consent.OPTED_IN
            },
            regulation.gdprConsentString
        )
        Vungle.updateUserCoppaStatus(regulation.coppaApplies)
    }

    override fun interstitial(): AdSource.Interstitial<VungleFullscreenAuctionParams> {
        return VungleInterstitialImpl()
    }

    override fun banner(): AdSource.Banner<VungleBannerAuctionParams> {
        return VungleBannerImpl()
    }

    override fun rewarded(): AdSource.Rewarded<VungleFullscreenAuctionParams> {
        return VungleRewardedImpl()
    }
}

private const val TAG = "VungleAdapter"