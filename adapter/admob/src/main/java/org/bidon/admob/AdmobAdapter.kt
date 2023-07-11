package org.bidon.admob

import android.content.Context
import com.google.android.gms.ads.MobileAds
import org.bidon.admob.ext.adapterVersion
import org.bidon.admob.ext.sdkVersion
import org.bidon.admob.impl.AdmobBannerImpl
import org.bidon.admob.impl.AdmobInterstitialImpl
import org.bidon.admob.impl.AdmobRewardedImpl
import org.bidon.sdk.adapter.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val AdmobDemandId = DemandId("admob")

@Suppress("unused")
class AdmobAdapter :
    Adapter,
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
