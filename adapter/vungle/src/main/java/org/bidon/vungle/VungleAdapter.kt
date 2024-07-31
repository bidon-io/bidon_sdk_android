package org.bidon.vungle

import android.content.Context
import com.vungle.ads.InitializationListener
import com.vungle.ads.VungleAds
import com.vungle.ads.VungleError
import com.vungle.ads.VunglePrivacySettings
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
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.logs.logging.impl.logError
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

/**
 * [Vungle Documentation](https://support.vungle.com/hc/en-us/articles/360002922871-Integrate-Vungle-SDK-for-Android-or-Amazon)
 */
class VungleAdapter :
    Adapter.Bidding,
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

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam) =
        VungleAds.getBiddingToken(context)

    override suspend fun init(context: Context, configParams: VungleParameters) = suspendCancellableCoroutine { continuation ->
        VungleAds.init(
            context,
            configParams.appId,
            object : InitializationListener {
                override fun onSuccess() {
                    continuation.resume(Unit)
                }

                override fun onError(vungleError: VungleError) {
                    logError(TAG, "Error while initialization", vungleError)
                    continuation.resumeWithException(vungleError)
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
        if (regulation.ccpaApplies) {
            VunglePrivacySettings.setCCPAStatus(regulation.hasCcpaConsent)
        }
        if (regulation.gdprApplies) {
            VunglePrivacySettings.setGDPRStatus(regulation.hasGdprConsent, null)
        }
        if (regulation.coppaApplies) {
            VunglePrivacySettings.setCOPPAStatus(true)
        }
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