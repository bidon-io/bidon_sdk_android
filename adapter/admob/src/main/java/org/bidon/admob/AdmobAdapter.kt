package org.bidon.admob

import android.content.Context
import com.google.android.gms.ads.MobileAds
import org.bidon.admob.ext.adapterVersion
import org.bidon.admob.ext.sdkVersion
import org.bidon.admob.impl.AdmobBannerImpl
import org.bidon.admob.impl.AdmobInterstitialImpl
import org.bidon.admob.impl.AdmobRewardedImpl
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal val AdmobDemandId = DemandId("admob")

@Suppress("unused")
internal class AdmobAdapter :
    Adapter.Network,
    Initializable<AdmobInitParameters>,
    AdProvider.Banner<AdmobBannerAuctionParams>,
    AdProvider.Rewarded<AdmobFullscreenAdAuctionParams>,
    AdProvider.Interstitial<AdmobFullscreenAdAuctionParams> {

    override val demandId = AdmobDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: AdmobInitParameters): Unit = suspendCoroutine { continuation ->
        // Since Bidon is the mediator, no need to initialize Google Bidding's partner SDKs.
        // https://developers.google.com/android/reference/com/google/android/gms/ads/MobileAds?hl=en#disableMediationAdapterInitialization(android.content.Context)
        // MobileAds.disableMediationAdapterInitialization(context)
        /**
         * Don't forget to disable automatic refresh for each AdUnit on the AdMob website.
         */
        MobileAds.initialize(context) {
            continuation.resume(Unit)
        }
    }

    override fun interstitial(): AdSource.Interstitial<AdmobFullscreenAdAuctionParams> {
        return AdmobInterstitialImpl()
    }

    override fun rewarded(): AdSource.Rewarded<AdmobFullscreenAdAuctionParams> {
        return AdmobRewardedImpl()
    }

    override fun banner(): AdSource.Banner<AdmobBannerAuctionParams> {
        return AdmobBannerImpl()
    }

    override fun parseConfigParam(json: String): AdmobInitParameters {
        val jsonObject = JSONObject(json)
        return AdmobInitParameters(
            requestAgent = jsonObject.optString("request_agent"),
            queryInfoType = jsonObject.optString("query_info_type")
        )
    }
}
