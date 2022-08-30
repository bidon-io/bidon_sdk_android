package com.appodealstack.admob

import android.app.Activity
import android.content.Context
import com.appodealstack.admob.ext.adapterVersion
import com.appodealstack.admob.ext.sdkVersion
import com.appodealstack.admob.impl.AdmobBannerImpl
import com.appodealstack.admob.impl.AdmobInterstitialImpl
import com.appodealstack.admob.impl.AdmobRewardedImpl
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.google.android.gms.ads.*
import kotlinx.serialization.json.JsonObject
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val AdmobDemandId = DemandId("admob")

class AdmobAdapter :
    Adapter,
    Initializable<AdmobInitParameters>,
    AdProvider.Banner<AdmobBannerAuctionParams>,
    AdProvider.Rewarded<AdmobFullscreenAdAuctionParams>,
    AdProvider.Interstitial<AdmobFullscreenAdAuctionParams> {
    private lateinit var context: Context

    override val demandId = AdmobDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: AdmobInitParameters): Unit = suspendCoroutine { continuation ->
        this.context = activity.applicationContext
        /**
         * Don't forget set Automatic refresh is Disabled for each AdUnit.
         * Manage refresh rate with [AutoRefresher.setAutoRefresh].
         */
        MobileAds.initialize(context) {
            continuation.resume(Unit)
        }
    }

    override fun interstitial(demandAd: DemandAd, roundId: String): AdSource.Interstitial<AdmobFullscreenAdAuctionParams> {
        return AdmobInterstitialImpl(demandId, demandAd, roundId)
    }

    override fun rewarded(demandAd: DemandAd, roundId: String): AdSource.Rewarded<AdmobFullscreenAdAuctionParams> {
        return AdmobRewardedImpl(demandId, demandAd, roundId)
    }

    override fun banner(demandAd: DemandAd, roundId: String): AdSource.Banner<AdmobBannerAuctionParams> {
        return AdmobBannerImpl(demandId, demandAd, roundId)
    }

    override fun parseConfigParam(json: JsonObject): AdmobInitParameters = AdmobInitParameters
}
