package com.appodealstack.admob

import android.app.Activity
import android.content.Context
import com.appodealstack.admob.ext.adapterVersion
import com.appodealstack.admob.ext.sdkVersion
import com.appodealstack.admob.impl.AdmobBannerImpl
import com.appodealstack.admob.impl.AdmobInterstitialImpl
import com.appodealstack.admob.impl.AdmobRewardedImpl
import com.appodealstack.bidon.data.models.config.AdapterInfo
import com.appodealstack.bidon.domain.adapter.AdProvider
import com.appodealstack.bidon.domain.adapter.AdSource
import com.appodealstack.bidon.domain.adapter.Adapter
import com.appodealstack.bidon.domain.adapter.Initializable
import com.appodealstack.bidon.domain.common.DemandAd
import com.appodealstack.bidon.domain.common.DemandId
import com.appodealstack.bidon.view.BannerView
import com.google.android.gms.ads.MobileAds
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
         * Manage refresh rate with [BannerView.startAutoRefresh].
         */
        MobileAds.initialize(context) {
            continuation.resume(Unit)
        }
    }

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<AdmobFullscreenAdAuctionParams> {
        return AdmobInterstitialImpl(
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
    ): AdSource.Rewarded<AdmobFullscreenAdAuctionParams> {
        return AdmobRewardedImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }

    override fun banner(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Banner<AdmobBannerAuctionParams> {
        return AdmobBannerImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }

    override fun parseConfigParam(json: String): AdmobInitParameters = AdmobInitParameters
}
