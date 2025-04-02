package org.bidon.gam

import android.content.Context
import com.google.android.gms.ads.MobileAds
import org.bidon.gam.ext.adapterVersion
import org.bidon.gam.ext.sdkVersion
import org.bidon.gam.impl.GamBannerImpl
import org.bidon.gam.impl.GamInterstitialImpl
import org.bidon.gam.impl.GamRewardedImpl
import org.bidon.sdk.adapter.*
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal val GamDemandId = DemandId("gam")

/**
 * [Google Ad Manager](https://developers.google.com/ad-manager/mobile-ads-sdk/android/quick-start)
 */
@Suppress("unused")
internal class GamAdapter :
    Adapter.Network,
    Initializable<GamInitParameters>,
    AdProvider.Banner<GamBannerAuctionParams>,
    AdProvider.Rewarded<GamFullscreenAdAuctionParams>,
    AdProvider.Interstitial<GamFullscreenAdAuctionParams> {

    override val demandId = GamDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: GamInitParameters): Unit = suspendCoroutine { continuation ->
        // Since Bidon is the mediator, no need to initialize Google Bidding's partner SDKs.
        // https://developers.google.com/android/reference/com/google/android/gms/ads/MobileAds?hl=en#disableMediationAdapterInitialization(android.content.Context)
        // MobileAds.disableMediationAdapterInitialization(context)
        /**
         * Don't forget set Automatic refresh is Disabled for each AdUnit.
         * Manage refresh rate with [BannerView.startAutoRefresh].
         */
        MobileAds.initialize(context) {
            continuation.resume(Unit)
        }
    }

    override fun interstitial(): AdSource.Interstitial<GamFullscreenAdAuctionParams> {
        return GamInterstitialImpl()
    }

    override fun rewarded(): AdSource.Rewarded<GamFullscreenAdAuctionParams> {
        return GamRewardedImpl()
    }

    override fun banner(): AdSource.Banner<GamBannerAuctionParams> {
        return GamBannerImpl()
    }

    override fun parseConfigParam(json: String): GamInitParameters {
        val jsonObject = JSONObject(json)
        return GamInitParameters(
            requestAgent = jsonObject.optString("request_agent"),
            queryInfoType = jsonObject.optString("query_info_type")
        )
    }
}